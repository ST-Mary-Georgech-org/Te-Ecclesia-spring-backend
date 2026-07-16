package org.teEcclesia.identity.service

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.teEcclesia.events.identity.UserCreatedEvent
import org.teEcclesia.events.identity.UserLoggedInEvent
import org.teEcclesia.events.identity.UserUpdatedEvent
import org.teEcclesia.events.identity.UserPendingApprovalEvent
import org.teEcclesia.events.publisher.TeEcclesiaEventPublisher
import org.teEcclesia.identity.api.dto.request.*
import org.teEcclesia.identity.api.dto.response.AuthResponse
import org.teEcclesia.identity.api.dto.response.RegisterResponse
import org.teEcclesia.identity.api.dto.response.InitiateWhatsAppVerificationResponse
import org.teEcclesia.identity.entity.*
import org.teEcclesia.identity.exception.InvalidCredentialsException
import org.teEcclesia.identity.exception.TokenExpiredException
import org.teEcclesia.identity.exception.UnauthorizedException
import org.teEcclesia.identity.exception.UserAlreadyExistsException
import org.teEcclesia.identity.exception.IncompleteProfileException
import org.teEcclesia.identity.exception.PhoneNotVerifiedException
import org.teEcclesia.identity.exception.AccountPendingApprovalException
import org.teEcclesia.identity.entity.enums.UserStatus
import org.teEcclesia.identity.repository.*
import org.teEcclesia.identity.security.JwtUtil
import org.teEcclesia.identity.service.mapper.WhatsAppWebhookMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.http.HttpMethod
import org.teEcclesia.client.ApiClient
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest
import java.net.URLEncoder
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.springframework.web.multipart.MultipartFile
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
    private val whatsAppWebhookMapper: WhatsAppWebhookMapper,
    private val apiClient: ApiClient,
    private val parentProfileService: ParentProfileService,
    private val rankRepository: RankRepository,
    private val educationalStageRepository: EducationalStageRepository,
    private val educationalYearRepository: EducationalYearRepository,
    private val imageStorageService: ImageStorageService,
    @param:Value("\${identity.resources.profile-image-directory}") private val profileImageDirectory: String,
    @param:Value("\${whatsapp.business-number}") private val whatsappBusinessNumber: String,
    @param:Value("\${whatsapp.business-phone}") private val whatsappBusinessPhone: String,
    @param:Value("\${whatsapp.app-secret:}") private val whatsappAppSecret: String,
    @param:Value("\${whatsapp.webhook.verify-token}") private val expectedVerifyToken: String,
    @param:Value("\${whatsapp.access-token:}") private val whatsappAccessToken: String,
    @param:Value("\${identity.resources.documents-directory}") private val documentsDirectory: String
) {

    val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun register(request: RegisterRequest, image: MultipartFile? = null, certificateImage: MultipartFile? = null) {
        val existingByPhone = userRepository.findByPhone(request.phone)
        if (existingByPhone != null && existingByPhone.isPhoneVerified) {
            throw UserAlreadyExistsException("Phone number is already registered and verified.")
        }

        val existingByNationalId = userRepository.findByNationalId(request.nationalId)
        if (existingByNationalId != null && existingByNationalId.isPhoneVerified) {
            if (existingByPhone == null || existingByPhone.id != existingByNationalId.id) {
                throw UserAlreadyExistsException("National ID is already registered.")
            }
        }

        if (!request.email.isNullOrBlank()) {
            val existingByEmail = userRepository.findByEmail(request.email.lowercase())
            if (existingByEmail != null && existingByEmail.isPhoneVerified) {
                if (existingByPhone == null || existingByPhone.id != existingByEmail.id) {
                    throw UserAlreadyExistsException("Email is already registered and verified.")
                }
            }
        }

        var confessionPriest: User? = null
        if (request.confessionPriestId != null) {
            confessionPriest = userRepository.findById(request.confessionPriestId).orElseThrow {
                EntityNotFoundException("Confession priest not found")
            }
        }

        val userId = existingByPhone?.id ?: UUID.randomUUID()
        
        val finalImageUrl = if (image != null) {
            imageStorageService.uploadImage(image, userId.toString(), profileImageDirectory)
        } else {
            request.imageUrl
        }

        val userToSave = request.toEntity(
            hashedPassword = passwordEncoder.encode(request.password)!!,
            confessionPriest = confessionPriest,
            id = userId,
            imageUrl = finalImageUrl
        )

        val finalCertificateUrl = if (certificateImage != null) {
            imageStorageService.uploadImage(certificateImage, "cert_${userId}", documentsDirectory)
        } else {
            request.ordinationProfile?.certificateImageUrl
        }

        val ordinationProfile = request.ordinationProfile?.let { createOrdinationProfile(userToSave, it, finalCertificateUrl) }
        val makhdoomProfile = request.makhdoomProfile?.let { createMakhdoomProfile(userToSave, it) }
        val savedUser = userRepository.save(userToSave.copy(
            ordinationProfile = ordinationProfile ?: userToSave.ordinationProfile,
            makhdoomProfile = makhdoomProfile ?: userToSave.makhdoomProfile
        ))

        request.parentProfile?.let { parentProfileService.createOrUpdateProfile(savedUser, it) }
    }

    fun completeProfile(request: CompleteProfileRequest): RegisterResponse {
        val user = findUserByIdentifier(request.identifier)

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw InvalidCredentialsException()
        }

        if (user.status != UserStatus.PROFILE_INCOMPLETE) {
            throw RuntimeException("Profile is already completed")
        }


        val ordinationProfile = request.ordinationProfile?.let { createOrdinationProfile(user, it, it.certificateImageUrl) }
        val makhdoomProfile = request.makhdoomProfile?.let { createMakhdoomProfile(user, it) }
        val khademProfile = request.khademProfile?.let { createKhademProfile(user, it) }

        val savedUser = userRepository.save(user.copy(
            status = UserStatus.UNVERIFIED,
            role = request.role,
            ordinationProfile = ordinationProfile ?: user.ordinationProfile,
            makhdoomProfile = makhdoomProfile ?: user.makhdoomProfile,
            khademProfile = khademProfile ?: user.khademProfile
        ))

        request.parentProfile?.let { parentProfileService.createOrUpdateProfile(savedUser, it) }

        val token = generateWhatsAppToken()
        val verificationToken = AccountVerification(
            otp = token,
            user = savedUser,
            phone = savedUser.phone,
            method = VerificationMethod.PHONE
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
        val user = userRepository.findByPhone(phone)
            ?: throw EntityNotFoundException("User not found with this phone number")

        if (user.isPhoneVerified) {
            throw RuntimeException("Phone number is already verified")
        }

        otpRepository.deleteAllByUserAndMethod(user, VerificationMethod.PHONE)

        val token = generateWhatsAppToken()
        val verificationToken = AccountVerification(
            otp = token,
            user = user,
            phone = user.phone,
            method = VerificationMethod.PHONE
        )
        otpRepository.save(verificationToken)

        val deepLink = buildWhatsAppLink(token)

        return InitiateWhatsAppVerificationResponse(
            deepLink = deepLink,
            token = token
        )
    }

    fun processWhatsAppWebhook(requestBody: String, signatureHeader: String?) {

        if (whatsappAppSecret.isNotBlank()) {
            val verified = verifyWebhookSignature(requestBody.toByteArray(), signatureHeader, whatsappAppSecret)
            if (!verified) {
                throw UnauthorizedException("Invalid webhook signature")
            }
        }

        val message = whatsAppWebhookMapper.parse(requestBody) ?: return

        val fromNumber = message.from
        val messageBody = message.body

        val tokenRegex = Regex("""AUTH_[A-Z0-9]{8}""")
        val matchResult = tokenRegex.find(messageBody) ?: return

        val tokenStr = matchResult.value

        val tokenEntity = otpRepository.findByOtpAndMethod(tokenStr, VerificationMethod.PHONE)
        if (tokenEntity == null) {
            sendWhatsAppMessage(fromNumber, "This verification code is invalid, expired, or has already been used.")
            return
        }

        if (tokenEntity.isExpired()) {
            otpRepository.delete(tokenEntity)
            sendWhatsAppMessage(fromNumber, "This verification code has expired. Please request a new one.")
            return
        }

        val cleanFrom = fromNumber.replace(Regex("""\D"""), "")
        val cleanRegistered = tokenEntity.user.phone.replace(Regex("""\D"""), "")

        if (cleanFrom != cleanRegistered) {
            sendWhatsAppMessage(cleanFrom, "Please send the verification message from your registered phone number.")
            return
        }

        val user = tokenEntity.user
        val wasUnverified = user.status == UserStatus.UNVERIFIED
        val verifiedUser = user.copy(
            isPhoneVerified = true,
            status = if (wasUnverified) UserStatus.PENDING_APPROVAL else user.status
        )
        userRepository.save(verifiedUser)
        teEcclesiaEventPublisher.publish(verifiedUser.toUserUpdatedEvent())

        if (wasUnverified) {
            teEcclesiaEventPublisher.publish(UserPendingApprovalEvent(verifiedUser.id, verifiedUser.fullName))
        }

        val approvedToken = tokenEntity.copy(otp = "APPROVED_$tokenStr")
        otpRepository.save(approvedToken)

        sendWhatsAppMessage(cleanFrom, "Your phone number has been successfully verified! Your account is now pending approval.")
    }

    private fun sendWhatsAppMessage(to: String, text: String) {
        if (whatsappAccessToken.isBlank()) return
        
        val url = "https://graph.facebook.com/v17.0/$whatsappBusinessNumber/messages"
        val payload = mapOf(
            "messaging_product" to "whatsapp",
            "to" to to,
            "type" to "text",
            "text" to mapOf("body" to text)
        )

        try {
            apiClient.call(String::class.java) {
                method = HttpMethod.POST
                path = url
                addToken = false
                headers["Authorization"] = "Bearer $whatsappAccessToken"
                body = payload
            }
        } catch (e: Exception) {
            logger.error("Failed to send WhatsApp message to $to: ${e.message}")
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

    fun verifyPhone(request: VerifyPhoneRequest): AuthResponse {
        return getWhatsAppStatus(request.otp)
    }

    fun verifyEmail(request: VerifyEmailRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email.lowercase())
            ?: throw EntityNotFoundException("User not found with this email")

        val token = otpRepository.findTopByOtpAndUserAndMethod(request.otp, user, VerificationMethod.EMAIL)
            ?: throw RuntimeException("Invalid or expired OTP")

        if (token.isExpired()) {
            otpRepository.delete(token)
            throw RuntimeException("OTP has expired")
        }

        val verifiedUser = user.copy(isEmailVerified = true)
        userRepository.save(verifiedUser)
        otpRepository.delete(token)

        val accessToken = jwtUtil.generateAccessToken(verifiedUser.id)
        val refreshToken = jwtUtil.generateRefreshToken(verifiedUser.id)
        saveRefreshToken(verifiedUser, refreshToken)

        return AuthResponse(accessToken, refreshToken)
    }

    fun login(request: LoginRequest): AuthResponse {
        val user = findUserByIdentifier(request.identifier)

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw InvalidCredentialsException()
        }

        when (user.status) {
            UserStatus.PROFILE_INCOMPLETE -> throw IncompleteProfileException()
            UserStatus.PENDING_APPROVAL -> throw AccountPendingApprovalException()
            UserStatus.UNVERIFIED -> {
                if (!user.isPhoneVerified) {
                    throw PhoneNotVerifiedException()
                }
            }
            UserStatus.REJECTED -> throw UnauthorizedException("Account rejected")
            UserStatus.BANNED -> throw UnauthorizedException("Account banned")
            UserStatus.APPROVED -> {
                // If it's approved but somehow phone is not verified, block them.
                if (!user.isPhoneVerified) {
                    throw PhoneNotVerifiedException()
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

    private fun saveRefreshToken(user: User, token: String, deviceToken: String? = null) {
        val expiryDate = Instant.now().plus(14, ChronoUnit.DAYS)

        refreshTokenRepository.save(
            RefreshToken(token = token, expiryDate = expiryDate, user = user, deviceToken = deviceToken)
        )
    }

    private fun findUserByIdentifier(identifier: String): User {
        val id = identifier.trim()
        return when {
            id.contains("@") -> userRepository.findByEmail(id.lowercase())
            id.matches(Regex("^\\d{14}$")) -> userRepository.findByNationalId(id)
            id.matches(Regex("^[A-H]\\d{8}$", RegexOption.IGNORE_CASE)) -> userRepository.findByCode(id.uppercase())
            id.matches(Regex("^\\+?\\d{10,15}$")) -> userRepository.findByPhone(id)
            else -> userRepository.findByPhone(id)
        } ?: throw EntityNotFoundException("User not found with this identifier")
    }

    private fun createOrdinationProfile(user: User, dto: OrdinationProfileRequest, finalCertificateUrl: String?): OrdinationProfile {
        val rank = rankRepository.findById(dto.rankId).orElseThrow {
            EntityNotFoundException("Rank not found")
        }
        return OrdinationProfile(
            user = user,
            rank = rank,
            isOrdinationInAnotherChurch = dto.isOrdinationInAnotherChurch,
            ordinationYear = dto.ordinationYear,
            bishopName = dto.bishopName,
            ordinationPlace = dto.ordinationPlace,
            certificateImageUrl = finalCertificateUrl
        )
    }

    private fun createMakhdoomProfile(user: User, dto: MakhdoomProfileRequest): MakhdoomProfile {
        val educationalStage = educationalStageRepository.findById(dto.educationalStageId).orElseThrow {
            EntityNotFoundException("Educational stage not found")
        }
        val educationalYear = dto.educationalYearId?.let {
            educationalYearRepository.findById(it).orElseThrow {
                EntityNotFoundException("Educational year not found")
            }
        }
        return MakhdoomProfile(
            user = user,
            shamamsaStudyStatus = dto.shamamsaStudyStatus,
            educationalStage = educationalStage,
            educationalYear = educationalYear,
            fatherPhone = dto.fatherPhone,
            fatherWhatsapp = dto.fatherWhatsapp,
            motherPhone = dto.motherPhone,
            motherWhatsapp = dto.motherWhatsapp,
            isFatherDeceased = dto.isFatherDeceased,
            isMotherDeceased = dto.isMotherDeceased
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
        return KhademProfile(
            user = user,
            educationalStage = educationalStage,
            educationalYear = educationalYear
        )
    }

    fun updateDeviceToken(userId: UUID, refreshToken: String, deviceToken: String) {
        val tokenEntity = refreshTokenRepository.findByUserIdAndToken(userId, refreshToken)
            ?: throw UnauthorizedException("Invalid refresh token")

        val updatedEntity = tokenEntity.copy(deviceToken = deviceToken)
        refreshTokenRepository.save(updatedEntity)
    }

    fun logout(userId: UUID, request: RefreshTokenRequest) {
        refreshTokenRepository.findByUserIdAndToken(userId = userId, request.refreshToken)?.let { tokenEntity ->
            refreshTokenRepository.delete(tokenEntity)
        }
    }

    fun forgotPassword(request: ForgotPasswordRequest): String {
        val user = findUserByKeyAndMethod(request.key, request.method)
            ?: return "If this user exists, an OTP has been sent."

        return if (request.method == VerificationMethod.PHONE) {
            val token = generateWhatsAppToken()
            val verification = AccountVerification(otp = token, user = user, phone = user.phone, method = VerificationMethod.PHONE)
            otpRepository.save(verification)
            buildWhatsAppLink(token)
        } else {
            val otpCode = emailService.generateOtp()
            val verification = AccountVerification(otp = otpCode, user = user, email = user.email, method = VerificationMethod.EMAIL)
            otpRepository.save(verification)
            if (user.email != null) {
                emailService.sendOtp(user.email, verification.otp)
            }
            "If this user exists, an OTP has been sent."
        }
    }

    fun verifyOtp(request: VerifyOtpRequest): String {
        val user = findUserByKeyAndMethod(request.key, request.method)
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
        val user = findUserByKeyAndMethod(request.key, request.method)
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

    fun resendOtp(request: ForgotPasswordRequest): String {
        val user = findUserByKeyAndMethod(request.key, request.method)
            ?: throw EntityNotFoundException("User not found")

        if (request.method == VerificationMethod.PHONE) {
            val token = generateWhatsAppToken()
            val verification = AccountVerification(otp = token, user = user, phone = user.phone, method = VerificationMethod.PHONE)
            otpRepository.save(verification)
            return buildWhatsAppLink(token)
        } else {
            val otpCode = emailService.generateOtp()
            val verificationToken = AccountVerification(otp = otpCode, user = user, email = user.email, method = VerificationMethod.EMAIL)
            otpRepository.save(verificationToken)
            if (user.email != null) {
                if (!user.isEmailVerified) {
                    emailService.sendWelcomeVerificationOtp(user.email, verificationToken.otp)
                } else {
                    emailService.sendOtp(user.email, verificationToken.otp)
                }
            }
            return "OTP resent successfully."
        }
    }

    private fun findUserByKeyAndMethod(key: String, method: VerificationMethod): User? {
        return if (method == VerificationMethod.PHONE) {
            userRepository.findByPhone(key)
        } else {
            userRepository.findByEmail(key.lowercase())
        }
    }

    private fun verifyWebhookSignature(payloadBytes: ByteArray, signatureHeader: String?, appSecret: String): Boolean {
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) return false
        val expectedSignature = signatureHeader.substringAfter("sha256=")
        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(appSecret.toByteArray(), "HmacSHA256")
        mac.init(secretKey)
        val actualSignatureBytes = mac.doFinal(payloadBytes)
        val actualSignature = actualSignatureBytes.joinToString("") { String.format("%02x", it) }
        return MessageDigest.isEqual(expectedSignature.toByteArray(), actualSignature.toByteArray())
    }

    fun getVerifyWebhookResponse(mode: String, verifyToken: String, challenge: String): String {
        if (mode == "subscribe" && verifyToken == expectedVerifyToken) {
            return challenge
        }
        throw UnauthorizedException("Webhook verification failed")
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
}