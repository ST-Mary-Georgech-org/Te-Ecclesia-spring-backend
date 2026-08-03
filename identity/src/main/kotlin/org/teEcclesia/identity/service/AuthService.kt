package org.teEcclesia.identity.service

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
import org.teEcclesia.identity.exception.AccountPendingApprovalException
import org.teEcclesia.identity.exception.DuplicatePhoneException
import org.teEcclesia.identity.entity.enums.UserStatus
import org.teEcclesia.identity.entity.lookups.Area
import org.teEcclesia.identity.repository.*
import org.teEcclesia.identity.security.JwtUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.springframework.web.multipart.MultipartFile
import org.teEcclesia.events.identity.UserApprovalRequestUpdatedEvent
import org.teEcclesia.identity.api.dto.response.PriestResponse
import org.teEcclesia.identity.api.dto.response.UserSummaryResponse
import org.teEcclesia.identity.api.dto.response.VerifyTokenResponse
import org.teEcclesia.identity.entity.enums.UserRole
import org.teEcclesia.identity.utils.formatPhone
import org.teEcclesia.storage.service.ImageStorageService
import java.util.*

@Service
@Transactional
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val otpRepository: EmailVerificationRepository,
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

        val existingUsersByPhone = userRepository.findUsersByPhone(formattedPhone)
        val verifiedPhoneUsers = existingUsersByPhone.filter { it.isPhoneVerified && it.id != existingUser?.id }

        if (verifiedPhoneUsers.size >= 2) {
            throw UserAlreadyExistsException("Phone number is already registered and verified twice.")
        }

        val matchingUnverifiedPhone = existingUsersByPhone.find { !it.isPhoneVerified && it.nationalId == request.nationalId }

        if (existingUser == null) {
            existingUser = matchingUnverifiedPhone
        } else if (matchingUnverifiedPhone != null && existingUser.id != matchingUnverifiedPhone.id) {
            userRepository.delete(matchingUnverifiedPhone)
        }

        userValidationHelper.validateEmail(request.email, existingUser?.id)


        var confessionPriest: User? = null
        if (request.confessionPriestId != null) {
            confessionPriest = userRepository.findById(request.confessionPriestId).orElseThrow {
                EntityNotFoundException("Confession priest not found")
            }
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
                    createdAt = existingUser.createdAt,
                    accountVerifications = existingUser.accountVerifications,
                    refreshTokens = existingUser.refreshTokens
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
            request.makhdoomProfile?.identityDocumentImageUrl ?: request.parentProfile?.nationalIdImageUrl
        }

        val ordinationProfile = request.ordinationProfile?.let { createOrdinationProfile(userToSave, it, finalCertificateUrl) }
        val makhdoomProfile = request.makhdoomProfile?.let { createMakhdoomProfile(userToSave, it, finalIdentityDocumentUrl) }
        val kahenProfile = request.kahenProfile?.let { createKahenProfile(userToSave, it) }
        val savedUser = userRepository.save(userToSave.copy(
            ordinationProfile = ordinationProfile ?: userToSave.ordinationProfile,
            makhdoomProfile = makhdoomProfile ?: userToSave.makhdoomProfile,
            kahenProfile = kahenProfile ?: userToSave.kahenProfile
        ))

        request.parentProfile?.let { parentProfileService.createOrUpdateProfile(savedUser, it, finalIdentityDocumentUrl) }

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
        val user = userRepository.findById(userId).orElseThrow { EntityNotFoundException("User not found") }

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
            request.makhdoomProfile?.identityDocumentImageUrl 
                ?: request.parentProfile?.nationalIdImageUrl 
                ?: user.makhdoomProfile?.identityDocumentImageUrl 
                ?: user.parentProfile?.nationalIdImageUrl
        }

        val ordinationProfile = request.ordinationProfile?.let { createOrdinationProfile(user, it, finalCertificateUrl) }
        val makhdoomProfile = request.makhdoomProfile?.let { createMakhdoomProfile(user, it, finalIdentityDocumentUrl) }
        val khademProfile = request.khademProfile?.let { createKhademProfile(user, it) }
        val kahenProfile = request.kahenProfile?.let { createKahenProfile(user, it) }

        val previousStatus = user.status
        val newStatus = if (user.isPhoneVerified) UserStatus.PENDING_APPROVAL else UserStatus.UNVERIFIED

        val savedUser = userRepository.save(user.copy(
            status = newStatus,
            role = request.role,
            ordinationProfile = ordinationProfile ?: user.ordinationProfile,
            makhdoomProfile = makhdoomProfile ?: user.makhdoomProfile,
            khademProfile = khademProfile ?: user.khademProfile,
            kahenProfile = kahenProfile ?: user.kahenProfile
        ))

        request.parentProfile?.let { parentProfileService.createOrUpdateProfile(savedUser, it, finalIdentityDocumentUrl) }

        if (previousStatus == UserStatus.PENDING_APPROVAL && newStatus == UserStatus.PENDING_APPROVAL) {
            teEcclesiaEventPublisher.publish(UserApprovalRequestUpdatedEvent(savedUser.id, savedUser.fullName))
        }

        val token = generateWhatsAppToken()
        val verificationToken = AccountVerification(
            otp = token,
            user = savedUser,
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
        val users = userRepository.findUsersByPhone(formattedPhone)
        val user = users.find { !it.isPhoneVerified }
            ?: if (users.isEmpty()) throw EntityNotFoundException("User not found with this phone number") else users[0]

        otpRepository.deleteAllByUserAndMethod(user, VerificationMethod.PHONE)

        val token = generateWhatsAppToken()
        val verificationToken = AccountVerification(
            otp = token,
            user = user,
            phone = user.phone,
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
        otpRepository.deleteAllByUserAndMethod(user, VerificationMethod.PHONE)
        val token = generateWhatsAppToken()
        val verificationToken = AccountVerification(
            otp = token,
            user = user,
            phone = newPhone,
            method = VerificationMethod.PHONE,
            purpose = VerificationPurpose.PHONE_CHANGE
        )
        otpRepository.save(verificationToken)
        return Pair(buildWhatsAppLink(token), token)
    }

    fun processWhatsAppVerification(tokenStr: String, fromNumber: String): VerifyTokenResponse {
        val tokenEntity = otpRepository.findByOtpAndMethod(tokenStr, VerificationMethod.PHONE)
            ?: return VerifyTokenResponse(false, "This verification code is invalid, expired, or has already been used.\nرمز التحقق هذا غير صالح أو منتهي الصلاحية أو تم استخدامه بالفعل.")

        if (tokenEntity.isExpired()) {
            otpRepository.delete(tokenEntity)
            return VerifyTokenResponse(false, "This verification code has expired. Please request a new one.\nانتهت صلاحية رمز التحقق هذا. يرجى طلب رمز جديد.")
        }

        if (!validateFromNumber(tokenEntity, fromNumber)) {
            return VerifyTokenResponse(false, "Please send the verification message from your registered phone number.\nيرجى إرسال رسالة التحقق من رقم هاتفك المسجل.")
        }

        updateVerifiedUser(tokenEntity)
        val responseMessage = getVerificationResponseMessage(tokenEntity)

        val approvedToken = tokenEntity.copy(otp = "APPROVED_$tokenStr")
        otpRepository.save(approvedToken)

        return VerifyTokenResponse(true, responseMessage)
    }

    private fun validateFromNumber(tokenEntity: AccountVerification, fromNumber: String): Boolean {
        val registeredPhone = tokenEntity.phone ?: tokenEntity.user.phone
        val cleanFrom = fromNumber.replace(Regex("""\D"""), "")
        val cleanRegistered = registeredPhone.replace(Regex("""\D"""), "")
        return cleanFrom == cleanRegistered
    }

    private fun updateVerifiedUser(tokenEntity: AccountVerification): User {
        val user = tokenEntity.user
        val verifiedUser = when (tokenEntity.purpose) {
            VerificationPurpose.PHONE_CHANGE -> {
                user.copy(
                    phone = tokenEntity.phone!!,
                    isPhoneVerified = true
                )
            }
            else -> {
                val wasUnverified = user.status == UserStatus.UNVERIFIED
                user.copy(
                    createdAt = Instant.now(),
                    isPhoneVerified = true,
                    status = if (wasUnverified) UserStatus.PENDING_APPROVAL else user.status
                )
            }
        }
        val savedUser = userRepository.save(verifiedUser)
        teEcclesiaEventPublisher.publish(savedUser.toUserUpdatedEvent())
        
        if (tokenEntity.purpose != VerificationPurpose.PHONE_CHANGE && user.status == UserStatus.UNVERIFIED) {
            teEcclesiaEventPublisher.publish(UserPendingApprovalEvent(savedUser.id, savedUser.fullName))
        }
        return savedUser
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

        val user = tokenEntity.user
        val accessToken = jwtUtil.generateAccessToken(user.id)
        val refreshToken = jwtUtil.generateRefreshToken(user.id)
        saveRefreshToken(user, refreshToken)

        otpRepository.delete(tokenEntity)

        return AuthResponse(accessToken, refreshToken)
    }

    fun verifyEmail(userId: UUID, request: VerifyEmailRequest): AuthResponse {
        val user = userRepository.findById(userId).orElseThrow {
            EntityNotFoundException("User not found with id: $userId")
        }

        val targetEmail = (if (!request.email.isNullOrBlank()) request.email else user.email)
            ?.lowercase()?.trim()
            ?: throw IllegalArgumentException("Email is required for verification")

        val token = otpRepository.findTopByOtpAndUserAndMethod(request.otp, user, VerificationMethod.EMAIL)
            ?: throw RuntimeException("Invalid or expired OTP")

        if (token.isExpired()) {
            otpRepository.delete(token)
            throw RuntimeException("OTP has expired")
        }

        userValidationHelper.validateEmail(targetEmail, currentUserId = userId)

        val verifiedUser = user.copy(
            email = targetEmail,
            isEmailVerified = true
        )
        userRepository.save(verifiedUser)
        otpRepository.delete(token)

        val accessToken = jwtUtil.generateAccessToken(verifiedUser.id)
        val refreshToken = jwtUtil.generateRefreshToken(verifiedUser.id)
        saveRefreshToken(verifiedUser, refreshToken, request.deviceToken)

        return AuthResponse(accessToken, refreshToken)
    }

    fun login(request: LoginRequest): AuthResponse {
        val identifier = request.identifier.trim()

        val formattedIdentifier = runCatching {
            formatPhone(identifier)
        }.getOrDefault(identifier)

        val users = userRepository.findUsersByIdentifier(formattedIdentifier)
        if (users.isEmpty()) {
            throw EntityNotFoundException("User not found with this identifier")
        }

        val matchingUsers = users.filter { passwordEncoder.matches(request.password, it.passwordHash) }

        if (matchingUsers.isEmpty()) {
            throw InvalidCredentialsException()
        }

        if (matchingUsers.size > 1) {
            throw InvalidCredentialsException("Ambiguous login. Multiple accounts match this identifier and password. Please use your National ID or user code instead.")
        }

        val user = matchingUsers[0]
        val isEmailIdentifier = user.email != null && identifier.equals(user.email, ignoreCase = true)

        if (isEmailIdentifier && !user.isEmailVerified) {
            throw EmailNotVerifiedException()
        }

        when (user.status) {
            UserStatus.PROFILE_INCOMPLETE -> {
                val tempToken = jwtUtil.generateRegistrationToken(user.id)
                val refreshToken = jwtUtil.generateRefreshToken(user.id)
                saveRefreshToken(user, refreshToken, request.deviceToken)
                throw IncompleteProfileException(token = tempToken, refreshToken = refreshToken)
            }
            UserStatus.PENDING_APPROVAL -> {
                val tempToken = jwtUtil.generateRegistrationToken(user.id)
                val refreshToken = jwtUtil.generateRefreshToken(user.id)
                saveRefreshToken(user, refreshToken, request.deviceToken)
                throw AccountPendingApprovalException(token = tempToken, refreshToken = refreshToken)
            }
            UserStatus.UNVERIFIED -> {
                if (!user.isPhoneVerified) {
                    val tempToken = jwtUtil.generateRegistrationToken(user.id)
                    val refreshToken = jwtUtil.generateRefreshToken(user.id)
                    saveRefreshToken(user, refreshToken, request.deviceToken)
                    throw PhoneNotVerifiedException(token = tempToken, refreshToken = refreshToken)
                }
            }
            UserStatus.REJECTED -> throw UnauthorizedException("Account rejected")
            UserStatus.BANNED -> throw UnauthorizedException("Account banned")
            UserStatus.APPROVED -> {
                // If it's approved but phone or email is not verified, block them.
                if (!user.isPhoneVerified) {
                    val tempToken = jwtUtil.generateRegistrationToken(user.id)
                    val refreshToken = jwtUtil.generateRefreshToken(user.id)
                    saveRefreshToken(user, refreshToken, request.deviceToken)
                    throw PhoneNotVerifiedException(token = tempToken, refreshToken = refreshToken)
                }
            }
        }

        val accessToken = jwtUtil.generateAccessToken(user.id)
        val refreshToken = jwtUtil.generateRefreshToken(user.id)

        saveRefreshToken(user, refreshToken, request.deviceToken)
        teEcclesiaEventPublisher.publish(UserLoggedInEvent(user.id))

        return AuthResponse(accessToken, refreshToken)
    }

    fun refreshToken(request: RefreshTokenRequest): AuthResponse {
        val refreshTokenEntity = refreshTokenRepository.findByToken(request.refreshToken)
            ?: throw UnauthorizedException("Invalid refresh token")

        val user = refreshTokenEntity.user
        val oldDeviceToken = refreshTokenEntity.deviceToken

        refreshTokenRepository.delete(refreshTokenEntity)

        if (refreshTokenEntity.expiryDate.isBefore(Instant.now())) {
            throw TokenExpiredException("Refresh token is expired. Please login again.")
        }

        if (jwtUtil.validateRefreshToken(request.refreshToken) &&
            jwtUtil.validateTokenForUser(request.refreshToken, user.id)) {

            val newAccessToken = jwtUtil.generateAccessToken(user.id)
            val newRefreshToken = jwtUtil.generateRefreshToken(user.id)

            val finalDeviceToken = request.deviceToken ?: oldDeviceToken
            saveRefreshToken(user, newRefreshToken, finalDeviceToken)
            teEcclesiaEventPublisher.publish(UserLoggedInEvent(user.id))

            return AuthResponse(newAccessToken, newRefreshToken)
        } else {
            throw UnauthorizedException("Invalid refresh token")
        }
    }

    fun refreshRegistrationToken(request: RefreshTokenRequest): AuthResponse {
        val refreshTokenEntity = refreshTokenRepository.findByToken(request.refreshToken)
            ?: throw UnauthorizedException("Invalid refresh token")

        val user = refreshTokenEntity.user
        val oldDeviceToken = refreshTokenEntity.deviceToken

        refreshTokenRepository.delete(refreshTokenEntity)

        if (refreshTokenEntity.expiryDate.isBefore(Instant.now())) {
            throw TokenExpiredException("Refresh token is expired. Please login again.")
        }

        if (jwtUtil.validateRefreshToken(request.refreshToken) &&
            jwtUtil.validateTokenForUser(request.refreshToken, user.id)) {

            val token = if (user.status == UserStatus.APPROVED) {
                jwtUtil.generateAccessToken(user.id)
            } else {
                jwtUtil.generateRegistrationToken(user.id)
            }
            val newRefreshToken = jwtUtil.generateRefreshToken(user.id)

            val finalDeviceToken = request.deviceToken ?: oldDeviceToken
            saveRefreshToken(user, newRefreshToken, finalDeviceToken)

            if (user.status == UserStatus.APPROVED) {
                teEcclesiaEventPublisher.publish(UserLoggedInEvent(user.id))
            }

            return AuthResponse(token, newRefreshToken)
        } else {
            throw UnauthorizedException("Invalid refresh token")
        }
    }

    fun upgradeRegistrationToken(userId: UUID): AuthResponse {
        val user = userRepository.findById(userId).orElseThrow {
            EntityNotFoundException("User not found")
        }
        if (user.status != UserStatus.APPROVED) {
            throw UnauthorizedException("User is not approved yet")
        }
        val accessToken = jwtUtil.generateAccessToken(user.id)
        val refreshToken = jwtUtil.generateRefreshToken(user.id)
        saveRefreshToken(user, refreshToken)
        teEcclesiaEventPublisher.publish(UserLoggedInEvent(user.id))
        return AuthResponse(accessToken, refreshToken)
    }

    private fun saveRefreshToken(user: User, token: String, deviceToken: String? = null) {
        val expiryDate = Instant.now().plus(14, ChronoUnit.DAYS)

        refreshTokenRepository.save(
            RefreshToken(token = token, expiryDate = expiryDate, user = user, deviceToken = deviceToken)
        )
    }

    private fun createOrdinationProfile(user: User, dto: OrdinationProfileRequest, finalCertificateUrl: String?): OrdinationProfile {
        val rank = rankRepository.findById(dto.rankId).orElseThrow {
            EntityNotFoundException("Rank not found")
        }
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

    private fun createMakhdoomProfile(user: User, dto: MakhdoomProfileRequest, finalIdentityDocumentUrl: String? = null): MakhdoomProfile {
        val educationalStage = educationalStageRepository.findById(dto.educationalStageId).orElseThrow {
            EntityNotFoundException("Educational stage not found")
        }
        if (educationalStage.isKhademOnly) {
            throw IllegalArgumentException("Educational stage is reserved for Khadem role")
        }
        val educationalYear = dto.educationalYearId?.let {
            educationalYearRepository.findById(it).orElseThrow {
                EntityNotFoundException("Educational year not found")
            }
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
            isMotherDeceased = dto.isMotherDeceased ?: false,
            identityDocumentImageUrl = finalIdentityDocumentUrl ?: dto.identityDocumentImageUrl ?: user.makhdoomProfile?.identityDocumentImageUrl
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
        val educationalStage = educationalStageRepository.findById(dto.educationalStageId).orElseThrow {
            EntityNotFoundException("Educational stage not found")
        }
        val educationalYear = dto.educationalYearId?.let { id ->
            educationalYearRepository.findById(id).orElseThrow {
                EntityNotFoundException("Educational year not found")
            }
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
        val tokenEntity = refreshTokenRepository.findByUserIdAndToken(userId, refreshToken)
        val entityToSave = if (tokenEntity == null) {
            val user = userRepository.getReferenceById(userId)
            val expiryDate = Instant.now().plus(14, ChronoUnit.DAYS)
            RefreshToken(token = refreshToken, expiryDate = expiryDate, user = user, deviceToken = deviceToken)
        } else {
            tokenEntity.copy(deviceToken = deviceToken)
        }
        refreshTokenRepository.save(entityToSave)
    }

    fun logout(userId: UUID, request: RefreshTokenRequest) {
        refreshTokenRepository.findByUserIdAndToken(userId = userId, request.refreshToken)?.let { tokenEntity ->
            refreshTokenRepository.delete(tokenEntity)
        }
    }

    fun forgotPassword(request: ForgotPasswordRequest): ForgotPasswordResponse? {
        val user = findUserForPasswordReset(request.key, request.method) ?: throw EntityNotFoundException("User not found or account is not approved yet")

        return if (request.method == VerificationMethod.PHONE) {
            val token = generateWhatsAppToken()
            val verification = AccountVerification(
                otp = token,
                user = user,
                phone = user.phone,
                method = VerificationMethod.PHONE,
                purpose = VerificationPurpose.PASSWORD_RESET
            )
            otpRepository.save(verification)
            ForgotPasswordResponse(buildWhatsAppLink(token), token)
        } else {
            val otpCode = emailService.generateOtp()
            val verification = AccountVerification(
                otp = otpCode,
                user = user,
                email = user.email,
                method = VerificationMethod.EMAIL,
                purpose = VerificationPurpose.PASSWORD_RESET
            )
            otpRepository.save(verification)
            if (user.email != null) {
                emailService.sendOtp(user.email, verification.otp)
            }
            null
        }
    }

    fun verifyOtp(request: VerifyOtpRequest): String {
        val user = findUserForPasswordReset(request.key, request.method)
            ?: throw EntityNotFoundException("User not found")

        val tokenStr = request.otp
        val approvedTokenStr = "APPROVED_$tokenStr"

        val token = otpRepository.findByOtpInAndMethod(listOf(tokenStr, approvedTokenStr), request.method)
            ?: throw RuntimeException("Invalid or expired OTP")

        if (token.user.id != user.id) {
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

        val tokenStr = request.otp
        val approvedTokenStr = "APPROVED_$tokenStr"

        val token = otpRepository.findByOtpInAndMethod(listOf(tokenStr, approvedTokenStr), request.method)
            ?: throw RuntimeException("Invalid OTP")

        if (token.user.id != user.id) {
            throw RuntimeException("Invalid OTP")
        }

        if (token.isExpired()) {
            otpRepository.delete(token)
            throw RuntimeException("Invalid or expired OTP")
        }

        if (request.method == VerificationMethod.PHONE && token.otp == tokenStr) {
            throw UnauthorizedException("Verification pending")
        }

        val updatedUser = user.copy(
            passwordHash = passwordEncoder.encode(request.newPassword)!!
        )
        userRepository.save(updatedUser)
        otpRepository.delete(token)

        return "Password reset successfully. You can now login."
    }

    fun resendOtp(request: ForgotPasswordRequest): ForgotPasswordResponse? {
        val user = findUserForPasswordReset(request.key, request.method)
            ?: throw EntityNotFoundException("User not found")

        return if (request.method == VerificationMethod.PHONE) {
            val token = generateWhatsAppToken()
            val verification = AccountVerification(
                otp = token,
                user = user,
                phone = user.phone,
                method = VerificationMethod.PHONE,
                purpose = VerificationPurpose.PASSWORD_RESET
            )
            otpRepository.save(verification)
            ForgotPasswordResponse(buildWhatsAppLink(token), token)
        } else {
            val otpCode = emailService.generateOtp()
            val verificationToken = AccountVerification(
                otp = otpCode,
                user = user,
                email = user.email,
                method = VerificationMethod.EMAIL,
                purpose = VerificationPurpose.PASSWORD_RESET
            )
            otpRepository.save(verificationToken)
            if (user.email != null) {
                if (!user.isEmailVerified) {
                    emailService.sendWelcomeVerificationOtp(user.email, verificationToken.otp)
                } else {
                    emailService.sendOtp(user.email, verificationToken.otp)
                }
            }
            null
        }
    }

    private fun findUserForPasswordReset(key: String, method: VerificationMethod): User? {
        if (method == VerificationMethod.EMAIL) {
            return userRepository.findByEmailAndStatus(key.lowercase(), UserStatus.APPROVED)
        }

        val isNationalId = key.matches(Regex("""\d{14}"""))
        if (isNationalId) {
            return userRepository.findByNationalIdAndStatus(key, UserStatus.APPROVED)
        }

        val formattedPhone = runCatching {
            formatPhone(key)
        }.getOrDefault(key)

        val usersByPhone = userRepository.findUsersByPhoneAndStatus(formattedPhone, UserStatus.APPROVED)
        if (usersByPhone.size > 1) {
            throw DuplicatePhoneException("This phone number is associated with multiple accounts. Please use your National ID to reset your password.")
        }
        return usersByPhone.firstOrNull()
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

    @Scheduled(cron = "0 0 0 * * *")
    fun clearExpiredRefreshTokens() {
        val now = Instant.now()
        refreshTokenRepository.deleteAllByExpiryDateBefore(now)
    }

    @Scheduled(cron = "0 0 0 * * *")
    fun clearExpiredOtps() {
        val now = Instant.now()
        otpRepository.deleteAllBySentAtBefore(now.minus(15, ChronoUnit.MINUTES))
    }

    @Scheduled(cron = "0 0 0 * * *")
    fun clearUnverifiedUsers() {
        val cutoffDate = Instant.now().minus(1, ChronoUnit.DAYS)
        userRepository.deleteAllByIsPhoneVerifiedIsFalseAndCreatedAtBefore(cutoffDate)
    }

    fun getConfessionPriests(pageable: Pageable): Page<PriestResponse> {
        val sort = if (pageable.sort.isUnsorted) Sort.by(Sort.Direction.ASC, "kahenProfile.ordinationDate") else pageable.sort
        val effectivePageable = PageRequest.of(pageable.pageNumber, pageable.pageSize, sort)
        val priestsPage = userRepository.findByRoleAndStatusIs(UserRole.KAHEN, UserStatus.APPROVED, effectivePageable)
        return priestsPage.map { priest ->
            PriestResponse(
                id = priest.id,
                name = priest.displayName,
                ordinationDate = priest.kahenProfile?.ordinationDate
            )
        }
    }

    fun searchParents(query: String, imagesBaseUrl: String): UserSummaryResponse? {
        val user = userRepository.findByRoleAndIdentifier(UserRole.PARENT, UserStatus.APPROVED, query).firstOrNull()
            ?: return null
        val fullImageUrl = user.imageUrl?.let { "$imagesBaseUrl/$it" }
        return UserSummaryResponse(
            id = user.id,
            code = user.code,
            name = user.displayName,
            imageUrl = fullImageUrl
        )
    }

    fun searchMakhdooms(query: String, imagesBaseUrl: String): UserSummaryResponse? {
        val user = userRepository.findByRoleAndIdentifier(UserRole.MAKHDOOM, UserStatus.APPROVED, query).firstOrNull()
            ?: return null
        val fullImageUrl = user.imageUrl?.let { "$imagesBaseUrl/$it" }
        return UserSummaryResponse(
            id = user.id,
            code = user.code,
            name = user.displayName,
            imageUrl = fullImageUrl
        )
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