package org.teEcclesia.identity.integration

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.teEcclesia.identity.IdentityTestApplication
import org.teEcclesia.identity.api.controller.InternalAuthController
import org.teEcclesia.identity.api.dto.request.PendingTokenRequest
import org.teEcclesia.identity.api.dto.request.VerifyTokenRequest
import org.teEcclesia.identity.api.dto.response.GetPendingTokenResponse
import org.teEcclesia.identity.api.dto.response.SavePendingTokenResponse
import org.teEcclesia.identity.entity.AccountVerification
import org.teEcclesia.identity.entity.User
import org.teEcclesia.identity.entity.VerificationMethod
import org.teEcclesia.identity.entity.enums.Gender
import org.teEcclesia.identity.entity.enums.UserRole
import org.teEcclesia.identity.entity.enums.UserStatus
import org.teEcclesia.identity.repository.EmailVerificationRepository
import org.teEcclesia.identity.repository.UserRepository
import org.teEcclesia.identity.repository.WhatsAppPendingTokenRepository
import java.time.LocalDate

@SpringBootTest(classes = [IdentityTestApplication::class])
@ActiveProfiles("test")
class InternalAuthControllerIntegrationTest {

    @Autowired
    private lateinit var internalAuthController: InternalAuthController

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var otpRepository: EmailVerificationRepository

    @Autowired
    private lateinit var whatsAppPendingTokenRepository: WhatsAppPendingTokenRepository

    @BeforeEach
    fun setUp() {
        whatsAppPendingTokenRepository.deleteAll()
        otpRepository.deleteAll()
        userRepository.deleteAll()
    }

    private fun createUser(phone: String = "+201012345678"): User {
        val user = User(
            firstName = "John",
            secondName = "Doe",
            thirdName = "Smith",
            lastName = "Johnson",
            displayName = "John Doe",
            nationalId = "29901010101010",
            phone = phone,
            email = "user@mail.com",
            passwordHash = "hashedPassword",
            job = "Developer",
            buildingNo = "10",
            street = "Main Street",
            area = "Downtown",
            floor = "2",
            apartment = "5",
            specialMark = "Near Park",
            birthDate = LocalDate.of(1990, 1, 1),
            gender = Gender.MALE,
            role = UserRole.MAKHDOOM,
            status = UserStatus.UNVERIFIED
        )
        return userRepository.save(user)
    }

    @Test
    fun `handlePendingToken saves token and retrieves it via get action`() {
        val userId = "bsuid_test_123"
        val token = "AUTH_PENDING_ABC"

        // 1. Save pending token
        val saveRequest = PendingTokenRequest(userId = userId, token = token, action = null)
        val saveResponse = internalAuthController.handlePendingToken(saveRequest)
        assertThat(saveResponse.body).isInstanceOf(SavePendingTokenResponse::class.java)
        val saveBody = saveResponse.body as SavePendingTokenResponse
        assertThat(saveBody.status).isEqualTo("saved")

        // 2. Get pending token
        val getRequest = PendingTokenRequest(userId = userId, token = null, action = "get")
        val getResponse = internalAuthController.handlePendingToken(getRequest)
        assertThat(getResponse.body).isInstanceOf(GetPendingTokenResponse::class.java)
        val getBody = getResponse.body as GetPendingTokenResponse
        assertThat(getBody.token).isEqualTo(token)
    }

    @Test
    fun `handlePendingToken returns null when token not found on get`() {
        val getRequest = PendingTokenRequest(userId = "non_existent_bsuid", token = null, action = "get")
        val getResponse = internalAuthController.handlePendingToken(getRequest)
        val getBody = getResponse.body as GetPendingTokenResponse
        assertThat(getBody.token).isNull()
    }

    @Test
    fun `verifyToken endpoint verifies token and clears pending token if userId is passed`() {
        val user = createUser("+201012345678")
        val token = "AUTH_WEBHOOK_TEST"
        val userId = "bsuid_verify_123"

        otpRepository.save(
            AccountVerification(
                otp = token,
                user = user,
                phone = user.phone,
                method = VerificationMethod.PHONE
            )
        )

        // Save pending token first
        internalAuthController.handlePendingToken(PendingTokenRequest(userId = userId, token = token, action = null))

        // Verify token
        val verifyRequest = VerifyTokenRequest(
            token = token,
            fromNumber = user.phone,
            userId = userId
        )
        val verifyResponse = internalAuthController.verifyToken(verifyRequest)
        val verifyBody = verifyResponse.body!!

        assertThat(verifyBody.verified).isTrue()
        assertThat(verifyBody.message).contains("Your phone number has been successfully verified!")
        assertThat(verifyBody.message).contains("تم التحقق من رقم هاتفك بنجاح!")

        // Verify pending token is cleared
        val getResponse = internalAuthController.handlePendingToken(PendingTokenRequest(userId = userId, token = null, action = "get"))
        val getBody = getResponse.body as GetPendingTokenResponse
        assertThat(getBody.token).isNull()
    }

    @Test
    fun `verifyToken endpoint returns failure when token is invalid`() {
        val verifyRequest = VerifyTokenRequest(
            token = "INVALID_TOKEN",
            fromNumber = "+201012345678",
            userId = null
        )
        val verifyResponse = internalAuthController.verifyToken(verifyRequest)
        val verifyBody = verifyResponse.body!!

        assertThat(verifyBody.verified).isFalse()
        assertThat(verifyBody.message).contains("This verification code is invalid, expired, or has already been used.")
        assertThat(verifyBody.message).contains("رمز التحقق هذا غير صالح أو منتهي الصلاحية أو تم استخدامه بالفعل.")
    }
}
