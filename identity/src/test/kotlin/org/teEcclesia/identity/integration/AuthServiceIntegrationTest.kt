package org.teEcclesia.identity.integration

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.teEcclesia.events.publisher.TeEcclesiaEventPublisher
import org.teEcclesia.identity.IdentityTestApplication
import org.teEcclesia.identity.api.dto.request.*
import org.teEcclesia.identity.entity.*
import org.teEcclesia.identity.exception.InvalidCredentialsException
import org.teEcclesia.identity.exception.TokenExpiredException
import org.teEcclesia.identity.exception.UnauthorizedException
import org.teEcclesia.identity.exception.UserAlreadyExistsException
import org.teEcclesia.identity.repository.EmailVerificationRepository
import org.teEcclesia.identity.repository.RefreshTokenRepository
import org.teEcclesia.identity.repository.UserRepository
import org.teEcclesia.identity.security.JwtUtil
import org.teEcclesia.identity.service.AuthService
import org.teEcclesia.identity.service.EmailService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@SpringBootTest(classes = [IdentityTestApplication::class])
@ActiveProfiles("test")
class AuthServiceIntegrationTest {

    @Autowired
    private lateinit var authService: AuthService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @Autowired
    private lateinit var otpRepository: EmailVerificationRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var emailService: EmailService

    @Autowired
    private lateinit var jwtUtil: JwtUtil

    @Autowired
    private lateinit var teEcclesiaEventPublisher: TeEcclesiaEventPublisher

    @BeforeEach
    fun setUp() {
        refreshTokenRepository.deleteAll()
        otpRepository.deleteAll()
        userRepository.deleteAll()
        every { emailService.generateOtp() } returns "12345"
        every { jwtUtil.generateAccessToken(any()) } returns "access-token"
        every { jwtUtil.generateRefreshToken(any()) } returns "refresh-token"
    }

    @Test
    fun `register returns WhatsApp link and token if user does not exist`() {
        val request = RegisterRequest(
            username = "newuser",
            fullName = "New User",
            phone = "123456789",
            email = "new-user@mail.com",
            password = "Password@1"
        )

        val response = authService.register(request)

        val savedUser = userRepository.findByPhone(request.phone)
        val savedOtp = savedUser?.let { otpRepository.findByOtpAndMethod(response.token, VerificationMethod.PHONE) }
        assertThat(response.message).isEqualTo("Registration successful. Please verify your phone number via WhatsApp.")
        assertThat(response.token).startsWith("AUTH_")
        assertThat(response.whatsappDeepLink).contains(response.token)
        assertThat(savedUser).isNotNull()
        assertThat(savedOtp).isNotNull()
        assertThat(savedOtp?.otp).isEqualTo(response.token)
    }

    @Test
    fun `register throw UserAlreadyExistsException if existing user is verified`() {
        val existingUser = createUser(email = "verified-user@mail.com", isVerified = true)
        val request = RegisterRequest(
            username = "verifieduser",
            fullName = "Verified User",
            phone = existingUser.phone,
            email = "different@mail.com",
            password = "Password@1"
        )

        val thrownException = assertThrows<UserAlreadyExistsException> { authService.register(request) }
        assertThat(thrownException).hasMessageThat().contains("Phone number is already registered and verified.")
    }

    @Test
    fun `register returns success message if existing user is unverified`() {
        val existingUser = createUser(email = "pending-user@mail.com", isVerified = false)
        val request = RegisterRequest(
            username = "pendinguser",
            fullName = "Pending Updated",
            phone = existingUser.phone,
            email = "pending-updated@mail.com",
            password = "Password@1"
        )

        val response = authService.register(request)

        val updatedUser = userRepository.findByPhone(existingUser.phone)
        assertThat(response.message).isEqualTo("Registration successful. Please verify your phone number via WhatsApp.")
        assertThat(updatedUser).isNotNull()
        assertThat(updatedUser?.id).isEqualTo(existingUser.id)
        assertThat(updatedUser?.fullName).isEqualTo("Pending Updated")
    }

    @Test
    fun `verifyPhone status check throw UnauthorizedException if pending`() {
        val user = createUser(email = "otp-invalid@mail.com", isVerified = false)
        val token = "AUTH_A1B2C3D4"
        otpRepository.save(
            AccountVerification(
                otp = token,
                user = user,
                phone = user.phone,
                method = VerificationMethod.PHONE
            )
        )

        val thrownException = assertThrows<UnauthorizedException> {
            authService.verifyPhone(VerifyPhoneRequest(phone = user.phone, otp = token))
        }
        assertThat(thrownException).hasMessageThat().contains("Verification pending")
    }

    @Test
    fun `verifyPhone status check returns auth response if approved`() {
        val user = createUser(email = "otp-valid@mail.com", isVerified = false)
        val token = "AUTH_A1B2C3D4"
        otpRepository.save(
            AccountVerification(
                otp = "APPROVED_$token",
                user = user,
                phone = user.phone,
                method = VerificationMethod.PHONE
            )
        )

        val authResponse = authService.verifyPhone(VerifyPhoneRequest(phone = user.phone, otp = token))

        assertThat(authResponse.accessToken).isEqualTo("access-token")
        assertThat(authResponse.refreshToken).isEqualTo("refresh-token")
        assertThat(otpRepository.findByOtpAndMethod("APPROVED_$token", VerificationMethod.PHONE)).isNull()
    }

    @Test
    fun `verifyEmail throw RuntimeException if otp is invalid`() {
        val user = createUser(email = "email-otp-invalid@mail.com", isVerified = false)
        val request = VerifyEmailRequest(email = "email-otp-invalid@mail.com", otp = "9999")

        val thrownException = assertThrows<RuntimeException> { authService.verifyEmail(request) }
        assertThat(thrownException).hasMessageThat().contains("Invalid or expired OTP")
    }

    @Test
    fun `verifyEmail throw RuntimeException if otp is expired`() {
        val user = createUser(email = "email-otp-expired@mail.com", isVerified = false)
        otpRepository.save(
            AccountVerification(
                otp = "12345",
                sentAt = Instant.now().minus(20, ChronoUnit.MINUTES),
                user = user,
                email = user.email,
                method = VerificationMethod.EMAIL
            )
        )
        val request = VerifyEmailRequest(email = user.email!!, otp = "12345")

        val thrownException = assertThrows<RuntimeException> { authService.verifyEmail(request) }
        assertThat(thrownException).hasMessageThat().contains("OTP has expired")
    }

    @Test
    fun `verifyEmail returns auth response if otp is valid`() {
        val user = createUser(email = "email-otp-valid@mail.com", isVerified = false)
        otpRepository.save(
            AccountVerification(
                otp = "12345",
                sentAt = Instant.now().minus(1, ChronoUnit.MINUTES),
                user = user,
                email = user.email,
                method = VerificationMethod.EMAIL
            )
        )
        val request = VerifyEmailRequest(email = user.email!!, otp = "12345")

        val authResponse = authService.verifyEmail(request)

        val verifiedUser = userRepository.findByEmail(user.email!!)
        val savedRefreshToken = refreshTokenRepository.findByToken("refresh-token")
        assertThat(authResponse.accessToken).isEqualTo("access-token")
        assertThat(authResponse.refreshToken).isEqualTo("refresh-token")
        assertThat(verifiedUser?.isEmailVerified).isTrue()
        assertThat(savedRefreshToken).isNotNull()
    }

    @Test
    fun `login throw InvalidCredentialsException if password is invalid`() {
        createUser(email = "invalid-password@mail.com", isVerified = true, plainPassword = "Password@1")
        val request = LoginRequest(username = "invalid-password", password = "Password@2")

        val thrownException = assertThrows<InvalidCredentialsException> { authService.login(request) }
        assertThat(thrownException).hasMessageThat().contains("Invalid username or password")
    }

    @Test
    fun `login throw UnauthorizedException if user is not verified`() {
        createUser(email = "not-verified@mail.com", isVerified = false, plainPassword = "Password@1")
        val request = LoginRequest(username = "not-verified", password = "Password@1")

        val thrownException = assertThrows<UnauthorizedException> { authService.login(request) }
        assertThat(thrownException).hasMessageThat().contains("Please verify your phone number before logging in.")
    }

    @Test
    fun `login returns auth response if credentials are valid and user is verified`() {
        createUser(email = "login-success@mail.com", isVerified = true, plainPassword = "Password@1")
        val request = LoginRequest(username = "login-success", password = "Password@1")

        val authResponse = authService.login(request)

        assertThat(authResponse.accessToken).isEqualTo("access-token")
        assertThat(authResponse.refreshToken).isEqualTo("refresh-token")
        assertThat(refreshTokenRepository.findByToken("refresh-token")).isNotNull()
    }

    @Test
    fun `processWhatsAppWebhook updates verification status`() {
        val user = createUser(email = "webhook-test@mail.com", isVerified = false)
        val token = "AUTH_T1T2T3T4"
        otpRepository.save(
            AccountVerification(
                otp = token,
                user = user,
                phone = user.phone,
                method = VerificationMethod.PHONE
            )
        )

        val jsonPayload = """
        {
          "object": "whatsapp_business_account",
          "entry": [
            {
              "id": "12345",
              "changes": [
                {
                  "value": {
                    "messaging_product": "whatsapp",
                    "messages": [
                      {
                        "from": "${user.phone}",
                        "text": {
                          "body": "Hi, please verify me: $token"
                        }
                      }
                    ]
                  },
                  "field": "messages"
                }
              ]
            }
          ]
        }
        """.trimIndent()

        authService.processWhatsAppWebhook(jsonPayload, signatureHeader = null)

        val updatedUser = userRepository.findById(user.id).get()
        val updatedToken = otpRepository.findByOtpAndMethod("APPROVED_$token", VerificationMethod.PHONE)
        assertThat(updatedUser.isPhoneVerified).isTrue()
        assertThat(updatedToken?.otp).isEqualTo("APPROVED_$token")
    }

    @Test
    fun `forgotPassword returns whatsapp link for phone request`() {
        val user = createUser(email = "forgot-verified@mail.com", isVerified = true)
        val request = ForgotPasswordRequest(key = user.phone, method = VerificationMethod.PHONE)

        val resultMessage = authService.forgotPassword(request)

        assertThat(resultMessage).startsWith("https://wa.me/")
        assertThat(resultMessage).contains("AUTH_")
    }

    private fun createUser(
        email: String,
        isVerified: Boolean,
        plainPassword: String = "Password@1",
        createdAt: Instant = Instant.now().minus(2, ChronoUnit.HOURS)
    ): User {
        val username = email.substringBefore("@")
        val phone = "12345" + UUID.randomUUID().toString().replace("-", "").take(6)
        val userToSave = User(
            username = username,
            fullName = "Integration User",
            email = email,
            phone = phone,
            passwordHash = passwordEncoder.encode(plainPassword)
                ?: throw IllegalStateException("Password encoding failed"),
            isEmailVerified = isVerified,
            isPhoneVerified = isVerified,
            createdAt = createdAt
        )
        return userRepository.save(userToSave)
    }
}
