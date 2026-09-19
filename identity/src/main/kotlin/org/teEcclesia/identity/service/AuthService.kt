package org.teEcclesia.identity.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.transaction.annotation.Transactional
import org.teEcclesia.events.identity.UserLoggedInEvent
import org.teEcclesia.events.identity.UserPendingApprovalEvent
import org.teEcclesia.events.publisher.TeEcclesiaEventPublisher
import org.teEcclesia.identity.api.dto.request.*
import org.teEcclesia.identity.api.dto.response.AuthResponse
import org.teEcclesia.identity.api.dto.response.ForgotPasswordResponse
import org.teEcclesia.identity.api.dto.response.RegisterResponse
import org.teEcclesia.identity.api.dto.response.TokenResponse
import org.teEcclesia.identity.api.dto.response.InitiateWhatsAppVerificationResponse
import org.teEcclesia.identity.entity.*
import org.teEcclesia.identity.exception.InvalidCredentialsException
import org.teEcclesia.identity.exception.TokenExpiredException
import org.teEcclesia.identity.exception.UnauthorizedException
import org.teEcclesia.identity.exception.UserAlreadyExistsException
import org.teEcclesia.identity.exception.IncompleteProfileException
import org.teEcclesia.identity.exception.PhoneNotVerifiedException
import org.teEcclesia.identity.exception.EmailNotVerifiedException
import org.teEcclesia.identity.exception.AccountDeletedException
import org.teEcclesia.identity.exception.AccountPendingApprovalException
import org.teEcclesia.identity.exception.DuplicatePhoneException
import org.teEcclesia.identity.exception.ResourceNotFoundException
import org.teEcclesia.identity.entity.enums.UserStatus
import org.teEcclesia.identity.entity.lookups.Area
import org.teEcclesia.events.notifications.NotificationDetails
import org.teEcclesia.events.notifications.UserNotificationsEvent
import org.teEcclesia.events.notifications.utils.NotificationAction
import org.teEcclesia.events.notifications.utils.NotificationMedium
import org.teEcclesia.events.notifications.utils.NotificationType
import org.teEcclesia.identity.repository.*
import org.teEcclesia.identity.repository.projection.UserAuthSummaryProjection
import org.teEcclesia.identity.repository.projection.UserPasswordResetProjection
import org.teEcclesia.identity.security.JwtUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.springframework.web.multipart.MultipartFile
import org.teEcclesia.events.identity.UserApprovalRequestUpdatedEvent
import org.teEcclesia.events.identity.UserUpdatedEvent
import org.teEcclesia.identity.api.dto.response.PriestResponse
import org.teEcclesia.identity.api.dto.response.UserSummaryResponse
import org.teEcclesia.identity.api.dto.response.toUserSummaryResponse
import org.teEcclesia.identity.api.dto.response.VerifyTokenResponse
import org.teEcclesia.identity.entity.enums.UserRole
import org.teEcclesia.identity.utils.formatPhone
import org.teEcclesia.storage.service.ImageStorageService
import org.teEcclesia.identity.entity.WhatsAppPendingToken
import org.teEcclesia.identity.repository.WhatsAppPendingTokenRepository
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Service
@Transactional(noRollbackFor = [
    AccountPendingApprovalException::class,
    IncompleteProfileException::class,
    PhoneNotVerifiedException::class
])
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val otpRepository: EmailVerificationRepository,
    private val whatsAppPendingTokenRepository: WhatsAppPendingTokenRepository,
    private val emailService: EmailService,
    private val passwordEncoder: PasswordEncoder,
    private val jwtUtil: JwtUtil,
    private val teEcclesiaEventPublisher: TeEcclesiaEventPublisher,
    private val parentProfileService: ParentProfileService,
    private val rankRepository: RankRepository,
    private val educationalStageRepository: EducationalStageRepository,
    private val educationalYearRepository: EducationalYearRepository,
    private val imageStorageService: ImageStorageService,
    private val areaRepository: AreaRepository,
    private val userValidationHelper: UserValidationHelper,
    private val accountDeletionRequestRepository: AccountDeletionRequestRepository,
    @param:Value("\${identity.resources.profile-image-directory}") private val profileImageDirectory: String,
    @param:Value("\${whatsapp.business-phone}") private val whatsappBusinessPhone: String,
    @param:Value("\${identity.resources.documents-directory}") private val documentsDirectory: String
) {

    private fun addAreaIfNotExists(areaName: String) {
        val area = areaName.trim()
        if (area.isNotEmpty()) {
            val existing = areaRepository.findByName(area)
            if (existing == null) {
                areaRepository.save(Area(name = area, suggestedCount = 1))
            }
        }
    }

    fun register(
        request: RegisterRequest,
        image: MultipartFile? = null,
        certificateImage: MultipartFile? = null,
        identityDocument: MultipartFile? = null
    ): TokenResponse {
        val formattedPhone = formatPhone(request.phone)
        
        var existingUser = userRepository.findFirstByNationalIdAndStatusNotIn(
            request.nationalId,
            listOf(UserStatus.REJECTED, UserStatus.BANNED)
        )

        if (existingUser?.status == UserStatus.APPROVED) {
            throw UserAlreadyExistsException("National ID is already registered.")
        }

        if (existingUser == null) {
            val deletedUser = userRepository.findDeletedByNationalId(request.nationalId)
            if (deletedUser != null) {
                throw AccountDeletedException("This account was previously deleted and can be reactivated.")
            }
        }

        val verifiedPhoneCount = userRepository.countVerifiedUsersByPhone(
            phone = formattedPhone,
            excludeUserId = existingUser?.id
        )

        if (verifiedPhoneCount >= 3) {
            throw UserAlreadyExistsException("Phone number is already registered and verified 3 times.")
        }

        val matchingUnverifiedPhone = userRepository.findFirstByPhoneAndNationalIdAndIsPhoneVerifiedFalse(
            phone = formattedPhone,
            nationalId = request.nationalId
        )

        if (existingUser == null) {
            existingUser = matchingUnverifiedPhone
        } else if (matchingUnverifiedPhone != null && existingUser.id != matchingUnverifiedPhone.id) {
            userRepository.delete(matchingUnverifiedPhone)
        }

        userValidationHelper.validateEmail(request.email, existingUser?.id)


        var confessionPriest: User? = null
        if (request.confessionPriestId != null) {
            if (!userRepository.existsById(request.confessionPriestId)) {
                throw EntityNotFoundException("Confession priest not found")
            }
            confessionPriest = userRepository.findProfileById(request.confessionPriestId)
        }

        val userId = existingUser?.id ?: UUID.randomUUID()
        
        val finalImageUrl = if (image != null) {
            imageStorageService.uploadImage(image, userId.toString(), profileImageDirectory)
        } else {
            request.imageUrl ?: existingUser?.imageUrl
        }

        val passwordToEncode = if (request.password.isNullOrBlank()) {
            generateRandomPassword()
        } else {
            request.password
        }

        val userToSave = request.toEntity(
            hashedPassword = passwordEncoder.encode(passwordToEncode)!!,
            confessionPriest = confessionPriest,
            id = userId,
            imageUrl = finalImageUrl
        ).let {
            if (existingUser != null) {
                it.copy(
                    isPhoneVerified = if (formattedPhone == existingUser.phone) existingUser.isPhoneVerified else false,
                    isEmailVerified = if (!request.email.isNullOrBlank() && request.email.equals(existingUser.email, ignoreCase = true)) existingUser.isEmailVerified else false,
                    status = existingUser.status,
                    role = existingUser.role.takeIf { r -> r != UserRole.GUEST } ?: (request.role ?: UserRole.GUEST),
                    ordinationProfile = existingUser.ordinationProfile,
                    makhdoomProfile = existingUser.makhdoomProfile,
                    khademProfile = existingUser.khademProfile,
                    parentProfile = existingUser.parentProfile,
                    createdAt = existingUser.createdAt
                )
            } else {
                it
            }
        }

        val finalCertificateUrl = if (certificateImage != null) {
            imageStorageService.uploadImage(certificateImage, "cert_${userId}", documentsDirectory)
        } else {
            request.ordinationProfile?.certificateImageUrl
        }

        val finalIdentityDocumentUrl = if (identityDocument != null) {
            imageStorageService.uploadImage(identityDocument, "id_${userId}", documentsDirectory)
        } else {
            request.identityDocumentImageUrl
        }

        val ordinationProfile = request.ordinationProfile?.let { createOrdinationProfile(userToSave, it, finalCertificateUrl) }
        val makhdoomProfile = request.makhdoomProfile?.let { createMakhdoomProfile(userToSave, it) }
        val khademProfile = request.khademProfile?.let { createKhademProfile(userToSave, it) }
        val kahenProfile = request.kahenProfile?.let { createKahenProfile(userToSave, it) }
        val savedUser = userRepository.save(userToSave.copy(
            identityDocumentImageUrl = finalIdentityDocumentUrl,
            ordinationProfile = ordinationProfile ?: userToSave.ordinationProfile,
            makhdoomProfile = makhdoomProfile ?: userToSave.makhdoomProfile,
            khademProfile = khademProfile ?: userToSave.khademProfile,
            kahenProfile = kahenProfile ?: userToSave.kahenProfile
        ))

        request.parentProfile?.let { parentProfileService.createOrUpdateProfile(savedUser, it) }

        addAreaIfNotExists(savedUser.area)

        val regToken = jwtUtil.generateRegistrationToken(savedUser.id)
        val refreshToken = jwtUtil.generateRefreshToken(savedUser.id)
        saveRefreshToken(savedUser, refreshToken)

        return TokenResponse(token = regToken, refreshToken = refreshToken)
    }

    fun completeProfile(
        userId: UUID,
        request: CompleteProfileRequest,
        certificateImage: MultipartFile? = null,
        identityDocument: MultipartFile? = null
    ): RegisterResponse {
        val user = userRepository.findProfileById(userId)
            ?: throw EntityNotFoundException("User not found")

        if (user.status == UserStatus.APPROVED) {
            throw RuntimeException("Profile is already completed")
        }

        val finalCertificateUrl = if (certificateImage != null) {
            imageStorageService.uploadImage(certificateImage, "cert_${user.id}", documentsDirectory)
        } else {
            request.ordinationProfile?.certificateImageUrl ?: user.ordinationProfile?.certificateImageUrl
        }

        val finalIdentityDocumentUrl = if (identityDocument != null) {
            imageStorageService.uploadImage(identityDocument, "id_${user.id}", documentsDirectory)
        } else {
            request.identityDocumentImageUrl ?: user.identityDocumentImageUrl
        }

        val ordinationProfile = request.ordinationProfile?.let { createOrdinationProfile(user, it, finalCertificateUrl) }
        val makhdoomProfile = request.makhdoomProfile?.let { createMakhdoomProfile(user, it) }
        val khademProfile = request.khademProfile?.let { createKhademProfile(user, it) }
        val kahenProfile = request.kahenProfile?.let { createKahenProfile(user, it) }

        val previousStatus = user.status
        val newStatus = if (user.isPhoneVerified) UserStatus.PENDING_APPROVAL else UserStatus.UNVERIFIED

        val savedUser = userRepository.save(user.copy(
            status = newStatus,
            role = request.role,
            identityDocumentImageUrl = finalIdentityDocumentUrl,
            ordinationProfile = ordinationProfile ?: user.ordinationProfile,
            makhdoomProfile = makhdoomProfile ?: user.makhdoomProfile,
            khademProfile = khademProfile ?: user.khademProfile,
            kahenProfile = kahenProfile ?: user.kahenProfile
        ))

        request.parentProfile?.let { parentProfileService.createOrUpdateProfile(savedUser, it) }

        if (previousStatus == UserStatus.PENDING_APPROVAL && newStatus == UserStatus.PENDING_APPROVAL) {
            teEcclesiaEventPublisher.publish(UserApprovalRequestUpdatedEvent(savedUser.id, savedUser.fullName))
        }

        val token = generateWhatsAppToken()
        val verificationToken = AccountVerification(
            otp = token,
            userId = savedUser.id,
            phone = savedUser.phone,
            method = VerificationMethod.PHONE,
            purpose = VerificationPurpose.REGISTER
        )
        otpRepository.save(verificationToken)

        val deepLink = buildWhatsAppLink(token)

        return RegisterResponse(
            message = "Profile completed successfully. Please verify your phone number via WhatsApp.",
            whatsappDeepLink = deepLink,
            token = token
        )
    }

    fun initiateWhatsAppVerification(phone: String): InitiateWhatsAppVerificationResponse {
        val formattedPhone = formatPhone(phone)
        val users = userRepository.findUsersByPhone(formattedPhone, UserAuthSummaryProjection::class.java)
        val user = users.find { !it.getIsPhoneVerified() }
            ?: if (users.isEmpty()) throw EntityNotFoundException("User not found with this phone number") else users[0]

        val userId = user.getId()
        otpRepository.deleteAllByUserIdAndMethod(userId, VerificationMethod.PHONE)

        val token = generateWhatsAppToken()
        val verificationToken = AccountVerification(
            otp = token,
            userId = userId,
            phone = user.getPhone(),
            method = VerificationMethod.PHONE,
            purpose = VerificationPurpose.REGISTER
        )
        otpRepository.save(verificationToken)

        val deepLink = buildWhatsAppLink(token)

        return InitiateWhatsAppVerificationResponse(
            deepLink = deepLink,
            token = token
        )
    }

    fun initiateWhatsAppPhoneChangeVerification(user: User, newPhone: String): Pair<String, String> {
        otpRepository.deleteAllByUserIdAndMethod(user.id, VerificationMethod.PHONE)
        val token = generateWhatsAppToken()
        val verificationToken = AccountVerification(
            otp = token,
            userId = user.id,
            phone = newPhone,
            method = VerificationMethod.PHONE,
            purpose = VerificationPurpose.PHONE_CHANGE
        )
        otpRepository.save(verificationToken)
        return Pair(buildWhatsAppLink(token), token)
    }

    fun savePendingToken(userId: String, token: String) {
        val pendingToken = WhatsAppPendingToken(
            userId = userId,
            token = token,
            createdAt = Instant.now(),
            expiresAt = Instant.now().plus(30, ChronoUnit.MINUTES)
        )
        whatsAppPendingTokenRepository.save(pendingToken)
    }

    fun getPendingToken(userId: String): String? {
        val pendingToken = whatsAppPendingTokenRepository.findByIdOrNull(userId) ?: return null
        if (pendingToken.isExpired()) {
            whatsAppPendingTokenRepository.delete(pendingToken)
            return null
        }
        return pendingToken.token
    }

    fun clearPendingToken(userId: String) {
        whatsAppPendingTokenRepository.deleteById(userId)
    }

    fun processWhatsAppVerification(tokenStr: String, fromNumber: String, userId: String? = null): VerifyTokenResponse {
        val tokenEntity = otpRepository.findByOtpAndMethod(tokenStr, VerificationMethod.PHONE)
            ?: return VerifyTokenResponse(verified = false, message = "This verification code is invalid, expired, or has already been used.\nرمز التحقق هذا غير صالح أو منتهي الصلاحية أو تم استخدامه بالفعل.")

        if (tokenEntity.isExpired()) {
            otpRepository.delete(tokenEntity)
            if (!userId.isNullOrBlank()) {
                clearPendingToken(userId)
            }
            return VerifyTokenResponse(verified = false, message = "This verification code has expired. Please request a new one.\nانتهت صلاحية رمز التحقق هذا. يرجى طلب رمز جديد.")
        }

        if (!validateFromNumber(tokenEntity, fromNumber)) {
            return VerifyTokenResponse(verified = false, message = "Please send the verification message from your registered phone number.\nيرجى إرسال رسالة التحقق من رقم هاتفك المسجل.")
        }

        updateVerifiedUser(tokenEntity)
        val responseMessage = getVerificationResponseMessage(tokenEntity)

        val approvedToken = tokenEntity.copy(otp = "APPROVED_$tokenStr")
        otpRepository.save(approvedToken)

        if (!userId.isNullOrBlank()) {
            clearPendingToken(userId)
        }

        return VerifyTokenResponse(verified = true, message = responseMessage)
    }

    private fun validateFromNumber(tokenEntity: AccountVerification, fromNumber: String): Boolean {
        val registeredPhone = tokenEntity.phone ?: userRepository.findPhoneById(tokenEntity.userId) ?: ""
        val cleanFrom = fromNumber.replace(Regex("""\D"""), "")
        val cleanRegistered = registeredPhone.replace(Regex("""\D"""), "")
        return cleanFrom == cleanRegistered
    }

    private fun updateVerifiedUser(tokenEntity: AccountVerification) {
        val userId = tokenEntity.userId
        val userBefore = userRepository.findAuthDetailsById(userId)
            ?: throw EntityNotFoundException("User not found")
        val previousStatus = userBefore.getStatus()

        if (tokenEntity.purpose == VerificationPurpose.PHONE_CHANGE) {
            userRepository.updatePhoneAndVerified(userId, tokenEntity.phone!!)
        } else {
            userRepository.verifyUserPhone(userId, Instant.now())
        }

        val updatedUserAuth = userRepository.findAuthDetailsById(userId)
            ?: throw EntityNotFoundException("User not found")

        val userUuid = UUID.fromString(updatedUserAuth.getId())

        teEcclesiaEventPublisher.publish(
            UserUpdatedEvent(
                id = userUuid,
                password = updatedUserAuth.getPasswordHash(),
                fullName = updatedUserAuth.getFullName(),
                imageUrl = updatedUserAuth.getImageUrl()
            )
        )

        if (tokenEntity.purpose != VerificationPurpose.PHONE_CHANGE) {
            if (previousStatus == UserStatus.UNVERIFIED.name && updatedUserAuth.getStatus() == UserStatus.PENDING_APPROVAL.name) {
                teEcclesiaEventPublisher.publish(UserPendingApprovalEvent(userUuid, updatedUserAuth.getFullName()))
            }
        }
    }

    private fun getVerificationResponseMessage(tokenEntity: AccountVerification): String {
        return when (tokenEntity.purpose) {
            VerificationPurpose.PHONE_CHANGE -> {
                "Your phone number has been successfully updated and verified!\nتم تحديث رقم هاتفك والتحقق منه بنجاح!"
            }
            VerificationPurpose.PASSWORD_RESET -> {
                "Your password reset request has been verified. Please return to the app to complete the process.\nتم التحقق من طلب إعادة تعيين كلمة المرور. يرجى العودة إلى التطبيق لإكمال العملية."
            }
            else -> {
                "Your phone number has been successfully verified! Your account is now pending approval.\nتم التحقق من رقم هاتفك بنجاح! حسابك الآن قيد الموافقة."
            }
        }
    }

    fun getWhatsAppStatus(token: String): AuthResponse {
        val approvedTokenStr = "APPROVED_$token"
        val tokenEntity = otpRepository.findByOtpInAndMethod(listOf(token, approvedTokenStr), VerificationMethod.PHONE)
            ?: throw EntityNotFoundException("Verification token not found or already processed")

        if (tokenEntity.otp == token) {
            throw UnauthorizedException("Verification pending")
        }

        if (tokenEntity.isExpired()) {
            otpRepository.delete(tokenEntity)
            throw RuntimeException("Verification token has expired")
        }

        val userAuth = userRepository.findAuthDetailsById(tokenEntity.userId)
            ?: throw EntityNotFoundException("User not found")
        val userUuid = UUID.fromString(userAuth.getId())
        val accessToken = jwtUtil.generateAccessToken(userUuid)
        val refreshToken = jwtUtil.generateRefreshToken(userUuid)
        saveRefreshToken(userUuid, refreshToken)

        otpRepository.delete(tokenEntity)

        return AuthResponse(accessToken, refreshToken)
    }

    fun verifyEmail(userId: UUID, request: VerifyEmailRequest): AuthResponse {
        val currentEmail = userRepository.findEmailById(userId)
        val targetEmail = (request.email.ifBlank { currentEmail ?: "" })
            .lowercase().trim()
            .takeIf { it.isNotEmpty() }
            ?: throw IllegalArgumentException("Email is required for verification")

        val token = otpRepository.findTopByOtpAndUserIdAndMethod(request.otp, userId, VerificationMethod.EMAIL)
            ?: throw RuntimeException("Invalid or expired OTP")

        if (token.isExpired()) {
            otpRepository.delete(token)
            throw RuntimeException("OTP has expired")
        }

        userValidationHelper.validateEmail(targetEmail, currentUserId = userId)

        userRepository.updateEmailAndVerified(userId, targetEmail)
        otpRepository.delete(token)

        val accessToken = jwtUtil.generateAccessToken(userId)
        val refreshToken = jwtUtil.generateRefreshToken(userId)
        saveRefreshToken(userId, refreshToken, request.deviceToken)

        return AuthResponse(accessToken, refreshToken)
    }

    fun login(request: LoginRequest): AuthResponse {
        val identifier = request.identifier.trim()

        val formattedIdentifier = runCatching {
            formatPhone(identifier)
        }.getOrDefault(identifier)

        val users = userRepository.findAuthSummariesByIdentifier(formattedIdentifier)
        if (users.isEmpty()) {
            throw EntityNotFoundException("User not found with this identifier")
        }

        val matchingUsers = users.filter { passwordEncoder.matches(request.password, it.getPasswordHash()) }

        if (matchingUsers.isEmpty()) {
            throw InvalidCredentialsException()
        }

        if (matchingUsers.size > 1) {
            throw InvalidCredentialsException("Ambiguous login. Multiple accounts match this identifier and password. Please use your National ID or user code instead.")
        }

        val user = matchingUsers[0]
        val isEmailIdentifier = user.getEmail() != null && identifier.equals(user.getEmail(), ignoreCase = true)

        if (isEmailIdentifier && !user.getIsEmailVerified()) {
            throw EmailNotVerifiedException()
        }

        val userId = user.getId()

        when (user.getStatus()) {
            UserStatus.PROFILE_INCOMPLETE -> {
                val tempToken = jwtUtil.generateRegistrationToken(userId)
                val refreshToken = jwtUtil.generateRefreshToken(userId)
                saveRefreshToken(userId, refreshToken, request.deviceToken)
                throw IncompleteProfileException(token = tempToken, refreshToken = refreshToken)
            }
            UserStatus.PENDING_APPROVAL -> {
                val tempToken = jwtUtil.generateRegistrationToken(userId)
                val refreshToken = jwtUtil.generateRefreshToken(userId)
                saveRefreshToken(userId, refreshToken, request.deviceToken)
                throw AccountPendingApprovalException(token = tempToken, refreshToken = refreshToken)
            }
            UserStatus.UNVERIFIED -> {
                if (!user.getIsPhoneVerified()) {
                    val tempToken = jwtUtil.generateRegistrationToken(userId)
                    val refreshToken = jwtUtil.generateRefreshToken(userId)
                    saveRefreshToken(userId, refreshToken, request.deviceToken)
                    throw PhoneNotVerifiedException(token = tempToken, refreshToken = refreshToken)
                }
            }
            UserStatus.REJECTED -> throw UnauthorizedException("Account rejected")
            UserStatus.BANNED -> throw UnauthorizedException("Account banned")
            UserStatus.APPROVED -> {
                // If it's approved but phone or email is not verified, block them.
                if (!user.getIsPhoneVerified()) {
                    val tempToken = jwtUtil.generateRegistrationToken(userId)
                    val refreshToken = jwtUtil.generateRefreshToken(userId)
                    saveRefreshToken(userId, refreshToken, request.deviceToken)
                    throw PhoneNotVerifiedException(token = tempToken, refreshToken = refreshToken)
                }
            }
        }

        val accessToken = jwtUtil.generateAccessToken(userId)
        val refreshToken = jwtUtil.generateRefreshToken(userId)

        saveRefreshToken(userId, refreshToken, request.deviceToken)
        teEcclesiaEventPublisher.publish(UserLoggedInEvent(userId))

        return AuthResponse(accessToken, refreshToken)
    }

    private data class RotatedTokenGraceEntry(
        val response: AuthResponse,
        val rotatedAt: Instant = Instant.now()
    )

    private val rotatedTokensGraceCache = ConcurrentHashMap<String, RotatedTokenGraceEntry>()

    private fun cleanExpiredGraceTokens() {
        val cutoff = Instant.now().minus(60, ChronoUnit.SECONDS)
        rotatedTokensGraceCache.entries.removeIf { it.value.rotatedAt.isBefore(cutoff) }
    }

    fun refreshToken(request: RefreshTokenRequest): AuthResponse {
        cleanExpiredGraceTokens()
        rotatedTokensGraceCache[request.refreshToken]?.let { graceEntry ->
            if (graceEntry.rotatedAt.plus(30, ChronoUnit.SECONDS).isAfter(Instant.now())) {
                return graceEntry.response
            } else {
                rotatedTokensGraceCache.remove(request.refreshToken)
            }
        }

        return synchronized(this) {
            rotatedTokensGraceCache[request.refreshToken]?.let { graceEntry ->
                if (graceEntry.rotatedAt.plus(30, ChronoUnit.SECONDS).isAfter(Instant.now())) {
                    return graceEntry.response
                }
            }

            val refreshTokenEntity = refreshTokenRepository.findByToken(request.refreshToken)
                ?: throw UnauthorizedException("Invalid refresh token")

            val userId = refreshTokenEntity.userId
            val oldDeviceToken = refreshTokenEntity.deviceToken

            refreshTokenRepository.delete(refreshTokenEntity)

            if (refreshTokenEntity.expiryDate.isBefore(Instant.now())) {
                throw TokenExpiredException("Refresh token is expired. Please login again.")
            }

            if (jwtUtil.validateRefreshToken(request.refreshToken) &&
                jwtUtil.validateTokenForUser(request.refreshToken, userId)) {

                val newAccessToken = jwtUtil.generateAccessToken(userId)
                val newRefreshToken = jwtUtil.generateRefreshToken(userId)

                val finalDeviceToken = request.deviceToken ?: oldDeviceToken
                saveRefreshToken(userId, newRefreshToken, finalDeviceToken)
                teEcclesiaEventPublisher.publish(UserLoggedInEvent(userId))

                val response = AuthResponse(newAccessToken, newRefreshToken)
                rotatedTokensGraceCache[request.refreshToken] = RotatedTokenGraceEntry(response)
                response
            } else {
                throw UnauthorizedException("Invalid refresh token")
            }
        }
    }

    fun refreshRegistrationToken(request: RefreshTokenRequest): AuthResponse {
        cleanExpiredGraceTokens()
        rotatedTokensGraceCache[request.refreshToken]?.let { graceEntry ->
            if (graceEntry.rotatedAt.plus(30, ChronoUnit.SECONDS).isAfter(Instant.now())) {
                return graceEntry.response
            } else {
                rotatedTokensGraceCache.remove(request.refreshToken)
            }
        }

        return synchronized(this) {
            rotatedTokensGraceCache[request.refreshToken]?.let { graceEntry ->
                if (graceEntry.rotatedAt.plus(30, ChronoUnit.SECONDS).isAfter(Instant.now())) {
                    return graceEntry.response
                }
            }

            val refreshTokenEntity = refreshTokenRepository.findByToken(request.refreshToken)
                ?: throw UnauthorizedException("Invalid refresh token")

            val userId = refreshTokenEntity.userId
            val oldDeviceToken = refreshTokenEntity.deviceToken

            refreshTokenRepository.delete(refreshTokenEntity)

            if (refreshTokenEntity.expiryDate.isBefore(Instant.now())) {
                throw TokenExpiredException("Refresh token is expired. Please login again.")
            }

            if (jwtUtil.validateRefreshToken(request.refreshToken) &&
                jwtUtil.validateTokenForUser(request.refreshToken, userId)) {

                val userAuth = userRepository.findAuthDetailsById(userId)
                    ?: throw UnauthorizedException("User not found")

                val token = if (userAuth.getStatus() == UserStatus.APPROVED.name) {
                    jwtUtil.generateAccessToken(userId)
                } else {
                    jwtUtil.generateRegistrationToken(userId)
                }
                val newRefreshToken = jwtUtil.generateRefreshToken(userId)

                val finalDeviceToken = request.deviceToken ?: oldDeviceToken
                saveRefreshToken(userId, newRefreshToken, finalDeviceToken)

                if (userAuth.getStatus() == UserStatus.APPROVED.name) {
                    teEcclesiaEventPublisher.publish(UserLoggedInEvent(userId))
                }

                val response = AuthResponse(token, newRefreshToken)
                rotatedTokensGraceCache[request.refreshToken] = RotatedTokenGraceEntry(response)
                response
            } else {
                throw UnauthorizedException("Invalid refresh token")
            }
        }
    }

    fun upgradeRegistrationToken(userId: UUID): AuthResponse {
        val userAuth = userRepository.findAuthDetailsById(userId)
            ?: throw EntityNotFoundException("User not found")
        if (userAuth.getStatus() != UserStatus.APPROVED.name) {
            throw UnauthorizedException("User is not approved yet")
        }
        val userUuid = UUID.fromString(userAuth.getId())
        val accessToken = jwtUtil.generateAccessToken(userUuid)
        val refreshToken = jwtUtil.generateRefreshToken(userUuid)
        saveRefreshToken(userUuid, refreshToken)
        teEcclesiaEventPublisher.publish(UserLoggedInEvent(userUuid))
        return AuthResponse(accessToken, refreshToken)
    }

    private fun saveRefreshToken(user: User, token: String, deviceToken: String? = null) {
        saveRefreshToken(user.id, token, deviceToken)
    }

    private fun saveRefreshToken(userId: UUID, token: String, deviceToken: String? = null) {
        val expiryDate = Instant.now().plus(14, ChronoUnit.DAYS)
        refreshTokenRepository.save(
            RefreshToken(
                token = token,
                expiryDate = expiryDate,
                userId = userId,
                deviceToken = deviceToken
            )
        )
    }

    private fun createOrdinationProfile(user: User, dto: OrdinationProfileRequest, finalCertificateUrl: String?): OrdinationProfile {
        val rank = rankRepository.findByIdOrNull(dto.rankId)
            ?: throw EntityNotFoundException("Rank not found")
        return OrdinationProfile(
            id = user.ordinationProfile?.id ?: 0,
            user = user,
            rank = rank,
            isOrdinationInAnotherChurch = dto.isOrdinationInAnotherChurch ?: false,
            ordinationYear = dto.ordinationYear,
            bishopName = dto.bishopName,
            ordinationPlace = dto.ordinationPlace,
            certificateImageUrl = finalCertificateUrl
        )
    }

    private fun createMakhdoomProfile(user: User, dto: MakhdoomProfileRequest): MakhdoomProfile {
        val educationalStage = educationalStageRepository.findByIdOrNull(dto.educationalStageId)
            ?: throw EntityNotFoundException("Educational stage not found")
        if (educationalStage.isKhademOnly) {
            throw IllegalArgumentException("Educational stage is reserved for Khadem role")
        }
        val educationalYear = dto.educationalYearId?.let {
            educationalYearRepository.findByIdOrNull(it)
                ?: throw EntityNotFoundException("Educational year not found")
        }
        return MakhdoomProfile(
            id = user.makhdoomProfile?.id ?: 0,
            user = user,
            shamamsaStudyStatus = dto.shamamsaStudyStatus,
            educationalStage = educationalStage,
            educationalYear = educationalYear,
            fatherPhone = dto.fatherPhone,
            fatherWhatsapp = dto.fatherWhatsapp,
            motherPhone = dto.motherPhone,
            motherWhatsapp = dto.motherWhatsapp,
            isFatherDeceased = dto.isFatherDeceased ?: false,
            isMotherDeceased = dto.isMotherDeceased ?: false
        )
    }

    private fun createKahenProfile(user: User, dto: KahenProfileRequest): KahenProfile {
        val stages = if (dto.educationalStageIds.isNotEmpty()) {
            educationalStageRepository.findAllById(dto.educationalStageIds)
        } else {
            emptyList()
        }
        return KahenProfile(
            id = user.kahenProfile?.id ?: 0,
            user = user,
            educationalStages = stages.toMutableList(),
            ordinationDate = dto.ordinationDate
        )
    }

    private fun createKhademProfile(user: User, dto: KhademProfileRequest): KhademProfile {
        val educationalStage = educationalStageRepository.findByIdOrNull(dto.educationalStageId)
            ?: throw EntityNotFoundException("Educational stage not found")
        val educationalYear = dto.educationalYearId?.let { id ->
            educationalYearRepository.findByIdOrNull(id)
                ?: throw EntityNotFoundException("Educational year not found")
        }
        val responsibleStages = dto.responsibleStageIds?.takeIf { it.isNotEmpty() }?.let {
            educationalStageRepository.findAllById(it)
        } ?: emptyList()
        val responsibleYears = dto.responsibleYearIds?.takeIf { it.isNotEmpty() }?.let {
            educationalYearRepository.findAllById(it)
        } ?: emptyList()
        return KhademProfile(
            id = user.khademProfile?.id ?: 0,
            user = user,
            educationalStage = educationalStage,
            educationalYear = educationalYear,
            canApproveRequests = dto.canApproveRequests ?: false,
            responsibleStages = responsibleStages.toMutableList(),
            responsibleYears = responsibleYears.toMutableList()
        )
    }

    fun updateDeviceToken(userId: UUID, refreshToken: String, deviceToken: String) {
        val updated = refreshTokenRepository.updateDeviceToken(userId, refreshToken, deviceToken)
        if (updated == 0) {
            val expiryDate = Instant.now().plus(14, ChronoUnit.DAYS)
            refreshTokenRepository.save(
                RefreshToken(
                    token = refreshToken,
                    expiryDate = expiryDate,
                    userId = userId,
                    deviceToken = deviceToken
                )
            )
        }
    }

    fun logout(userId: UUID, request: RefreshTokenRequest) {
        rotatedTokensGraceCache.remove(request.refreshToken)
        refreshTokenRepository.deleteByUserIdAndToken(userId, request.refreshToken)
    }

    fun forgotPassword(request: ForgotPasswordRequest): ForgotPasswordResponse {
        val user = findUserForPasswordReset(request.key, request.method) ?: throw EntityNotFoundException("User not found or account is not approved yet")
        val userId = UUID.fromString(user.getId())

        return if (request.method == VerificationMethod.PHONE) {
            val token = generateWhatsAppToken()
            val verification = AccountVerification(
                otp = token,
                userId = userId,
                phone = user.getPhone(),
                method = VerificationMethod.PHONE,
                purpose = VerificationPurpose.PASSWORD_RESET
            )
            otpRepository.save(verification)
            ForgotPasswordResponse(buildWhatsAppLink(token), token, isDeletedAccount = user.getDeleted())
        } else {
            val otpCode = emailService.generateOtp()
            val verification = AccountVerification(
                otp = otpCode,
                userId = userId,
                email = user.getEmail(),
                method = VerificationMethod.EMAIL,
                purpose = VerificationPurpose.PASSWORD_RESET
            )
            otpRepository.save(verification)
            if (user.getEmail() != null) {
                emailService.sendOtp(user.getEmail()!!, verification.otp)
            }
            ForgotPasswordResponse(link = null, token = null, isDeletedAccount = user.getDeleted())
        }
    }

    fun verifyOtp(request: VerifyOtpRequest): String {
        val user = findUserForPasswordReset(request.key, request.method)
            ?: throw EntityNotFoundException("User not found")
        val userId = UUID.fromString(user.getId())

        val tokenStr = request.otp
        val approvedTokenStr = "APPROVED_$tokenStr"

        val token = otpRepository.findByOtpInAndMethod(listOf(tokenStr, approvedTokenStr), request.method)
            ?: throw RuntimeException("Invalid or expired OTP")

        if (token.userId != userId) {
            throw RuntimeException("Invalid or expired OTP")
        }

        if (token.isExpired()) {
            otpRepository.delete(token)
            throw RuntimeException("OTP has expired")
        }

        if (request.method == VerificationMethod.PHONE && token.otp == tokenStr) {
            throw UnauthorizedException("Verification pending")
        }

        return "OTP verified successfully. You can now reset your password."
    }

    fun resetPassword(request: ResetPasswordRequest): String {
        val user = findUserForPasswordReset(request.key, request.method)
            ?: throw EntityNotFoundException("User not found")
        val userId = UUID.fromString(user.getId())

        val tokenStr = request.otp
        val approvedTokenStr = "APPROVED_$tokenStr"

        val token = otpRepository.findByOtpInAndMethod(listOf(tokenStr, approvedTokenStr), request.method)
            ?: throw RuntimeException("Invalid OTP")

        if (token.userId != userId) {
            throw RuntimeException("Invalid OTP")
        }

        if (token.isExpired()) {
            otpRepository.delete(token)
            throw RuntimeException("Invalid or expired OTP")
        }

        if (request.method == VerificationMethod.PHONE && token.otp == tokenStr) {
            throw UnauthorizedException("Verification pending")
        }

        if (user.getDeleted()) {
            userRepository.restoreUser(userId)
            accountDeletionRequestRepository.deleteByUserId(userId)

            val adminIds = userRepository.findAllAdminIds()
            if (adminIds.isNotEmpty()) {
                val notifications = adminIds.map { adminId ->
                    NotificationDetails(
                        userId = adminId,
                        subject = "إعادة تفعيل حساب",
                        message = "قام ${user.getFullName()} بإعادة تفعيل حسابه بنفسه عبر استعادة كلمة المرور.",
                        type = NotificationType.ALERT,
                        medium = NotificationMedium.PUSH,
                        dataPayload = mapOf("action" to NotificationAction.ACCOUNT_REACTIVATED.name, "targetUserId" to userId.toString())
                    )
                }
                teEcclesiaEventPublisher.publish(UserNotificationsEvent(notifications))
            }
        }

        userRepository.updatePasswordHash(userId, passwordEncoder.encode(request.newPassword)!!)
        otpRepository.delete(token)

        return "Password reset successfully. You can now login."
    }

    fun resendOtp(request: ForgotPasswordRequest): ForgotPasswordResponse {
        val user = findUserForPasswordReset(request.key, request.method)
            ?: throw EntityNotFoundException("User not found")
        val userId = UUID.fromString(user.getId())

        return if (request.method == VerificationMethod.PHONE) {
            val token = generateWhatsAppToken()
            val verification = AccountVerification(
                otp = token,
                userId = userId,
                phone = user.getPhone(),
                method = VerificationMethod.PHONE,
                purpose = VerificationPurpose.PASSWORD_RESET
            )
            otpRepository.save(verification)
            ForgotPasswordResponse(buildWhatsAppLink(token), token, isDeletedAccount = user.getDeleted())
        } else {
            val otpCode = emailService.generateOtp()
            val verificationToken = AccountVerification(
                otp = otpCode,
                userId = userId,
                email = user.getEmail(),
                method = VerificationMethod.EMAIL,
                purpose = VerificationPurpose.PASSWORD_RESET
            )
            otpRepository.save(verificationToken)
            if (user.getEmail() != null) {
                if (!user.getIsEmailVerified()) {
                    emailService.sendWelcomeVerificationOtp(user.getEmail()!!, verificationToken.otp)
                } else {
                    emailService.sendOtp(user.getEmail()!!, verificationToken.otp)
                }
            }
            ForgotPasswordResponse(link = null, token = null, isDeletedAccount = user.getDeleted())
        }
    }

    private fun findUserForPasswordReset(key: String, method: VerificationMethod): UserPasswordResetProjection? {
        val trimmedKey = key.trim()
        val formattedIdentifier = if (method == VerificationMethod.PHONE) {
            runCatching { formatPhone(trimmedKey) }.getOrDefault(trimmedKey)
        } else {
            trimmedKey
        }

        val users = userRepository.findForPasswordResetByIdentifier(formattedIdentifier)
        if (users.isEmpty()) {
            return null
        }

        if (method == VerificationMethod.PHONE && users.size > 1) {
            val phoneMatches = users.filter { it.getPhone() == formattedIdentifier }
            if (phoneMatches.size > 1) {
                throw DuplicatePhoneException("This phone number is associated with multiple accounts. Please use your National ID to reset your password.")
            }
        }

        return users.firstOrNull()
    }



    private fun generateWhatsAppToken(): String {
        val allowedChars = ('A'..'Z') + ('0'..'9')
        return "AUTH_" + (1..8)
            .map { allowedChars.random() }
            .joinToString("")
    }

    private fun buildWhatsAppLink(token: String): String {
        val message = "Verify my account: $token"
        val encodedMessage = URLEncoder.encode(message, "UTF-8")
        return "https://wa.me/$whatsappBusinessPhone?text=$encodedMessage"
    }

    @Transactional
    @Scheduled(cron = "0 0 0 * * *")
    fun clearExpiredRefreshTokens() {
        val now = Instant.now()
        refreshTokenRepository.deleteAllByExpiryDateBefore(now)
    }

    @Transactional
    @Scheduled(cron = "0 0 0 * * *")
    fun clearExpiredOtps() {
        val now = Instant.now()
        otpRepository.deleteAllBySentAtBefore(now.minus(15, ChronoUnit.MINUTES))
    }

    @Transactional
    @Scheduled(cron = "0 0 0 * * *")
    fun clearUnverifiedUsers() {
        val cutoffDate = Instant.now().minus(1, ChronoUnit.DAYS)
        userRepository.deleteAllByIsPhoneVerifiedIsFalseAndCreatedAtBefore(cutoffDate)
    }

    fun getConfessionPriests(pageable: Pageable): Page<PriestResponse> {
        val sort = if (pageable.sort.isUnsorted) Sort.by(Sort.Direction.ASC, "kahenProfile.ordinationDate") else pageable.sort
        val effectivePageable = PageRequest.of(pageable.pageNumber, pageable.pageSize, sort)
        val priestsPage = userRepository.findPriestsSummary(UserRole.KAHEN, UserStatus.APPROVED, effectivePageable)
        return priestsPage.map { priest ->
            PriestResponse(
                id = priest.getId(),
                name = priest.getName(),
                ordinationDate = priest.getOrdinationDate()
            )
        }
    }

    fun searchParents(query: String, imagesBaseUrl: String): UserSummaryResponse {
        val projection = userRepository.findSummaryByRolesAndIdentifier(
            listOf(UserRole.PARENT, UserRole.KHADEM, UserRole.KAHEN),
            UserStatus.APPROVED,
            query
        ).firstOrNull() ?: throw ResourceNotFoundException("No parent found matching: $query")

        return projection.toUserSummaryResponse(imagesBaseUrl)
    }

    fun searchMakhdooms(query: String, imagesBaseUrl: String): UserSummaryResponse {
        val projection = userRepository.findSummaryByRolesAndIdentifier(
            listOf(UserRole.MAKHDOOM, UserRole.KHADEM, UserRole.PARENT),
            UserStatus.APPROVED,
            query
        ).firstOrNull() ?: throw ResourceNotFoundException("No child found matching: $query")

        return projection.toUserSummaryResponse(imagesBaseUrl)
    }

    private fun generateRandomPassword(): String {
        val uppercase = ('A'..'Z').toList()
        val lowercase = ('a'..'z').toList()
        val digits = ('0'..'9').toList()
        val specials = listOf('!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '-', '_', '+', '=')
        val allChars = uppercase + lowercase + digits + specials

        val password = mutableListOf<Char>()
        password.add(uppercase.random())
        password.add(lowercase.random())
        password.add(digits.random())
        password.add(specials.random())

        repeat(4) {
            password.add(allChars.random())
        }

        password.shuffle()
        return password.joinToString("")
    }
}