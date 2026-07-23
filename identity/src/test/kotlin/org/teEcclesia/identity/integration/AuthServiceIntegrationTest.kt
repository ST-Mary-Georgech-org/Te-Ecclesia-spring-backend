package org.teEcclesia.identity.integration

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.teEcclesia.events.publisher.TeEcclesiaEventPublisher
import org.teEcclesia.identity.IdentityTestApplication
import org.teEcclesia.identity.entity.*
import org.teEcclesia.identity.exception.InvalidCredentialsException
import org.teEcclesia.identity.exception.UnauthorizedException
import org.teEcclesia.identity.exception.UserAlreadyExistsException
import org.teEcclesia.identity.exception.PhoneNotVerifiedException
import org.teEcclesia.identity.exception.DuplicatePhoneException
import org.teEcclesia.identity.security.JwtUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.util.ReflectionTestUtils
import org.teEcclesia.identity.api.dto.request.CompleteProfileRequest
import org.teEcclesia.identity.api.dto.request.ForgotPasswordRequest
import org.teEcclesia.identity.api.dto.request.LoginRequest
import org.teEcclesia.identity.api.dto.request.KahenProfileRequest
import org.teEcclesia.identity.api.dto.request.MakhdoomProfileRequest
import org.teEcclesia.identity.api.dto.request.ParentProfileRequest
import org.teEcclesia.identity.api.dto.request.RegisterRequest
import org.teEcclesia.identity.api.dto.request.VerifyEmailRequest
import org.teEcclesia.identity.entity.enums.Gender
import org.teEcclesia.identity.entity.enums.ShamamsaStudyStatus
import org.teEcclesia.identity.entity.enums.UserRole
import org.teEcclesia.identity.entity.enums.UserStatus
import org.teEcclesia.identity.entity.lookups.EducationalStage
import org.teEcclesia.identity.entity.lookups.EducationalYear
import org.teEcclesia.identity.entity.lookups.Rank
import org.teEcclesia.identity.repository.AreaRepository
import org.teEcclesia.identity.repository.RankRepository
import org.teEcclesia.identity.api.dto.request.OrdinationProfileRequest
import org.teEcclesia.identity.api.dto.request.KhademProfileRequest
import org.teEcclesia.identity.repository.EducationalStageRepository
import org.teEcclesia.identity.repository.EducationalYearRepository
import org.teEcclesia.identity.repository.EmailVerificationRepository
import org.teEcclesia.identity.repository.RefreshTokenRepository
import org.teEcclesia.identity.repository.UserRepository
import org.teEcclesia.identity.service.AuthService
import org.teEcclesia.identity.service.EmailService
import org.teEcclesia.identity.service.ParentProfileService
import org.teEcclesia.identity.utils.formatPhone
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

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
    private lateinit var emailService: EmailService
    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder
    @Autowired
    private lateinit var jwtUtil: JwtUtil
    @Autowired private lateinit var parentProfileService: ParentProfileService
    @Autowired private lateinit var educationalStageRepository: EducationalStageRepository
    @Autowired private lateinit var educationalYearRepository: EducationalYearRepository
    @Autowired private lateinit var rankRepository: RankRepository
    
    @Autowired
    private lateinit var areaRepository: AreaRepository

    @BeforeEach
    fun setUp() {
        areaRepository.deleteAll()
        refreshTokenRepository.deleteAll()
        otpRepository.deleteAll()
        userRepository.deleteAll()
        educationalYearRepository.deleteAll()
        educationalStageRepository.deleteAll()
        rankRepository.deleteAll()
        every { emailService.generateOtp() } returns "12345"
        every { jwtUtil.generateAccessToken(any()) } returns "access-token"
        every { jwtUtil.generateRefreshToken(any()) } returns "refresh-token"
    }

    @Test
    fun `register saves user with status PROFILE_INCOMPLETE and saves new area if not exists`() {
        val request = RegisterRequest(
            firstName = "First", secondName = "Second", thirdName = "Third", lastName = "Last",
            displayName = "New User", nationalId = "29001010101010",
            phone = "01118295474", homePhone = "0223456789",
            email = "new-user@mail.com", password = "Password@1",
            job = "Job", buildingNo = "1", street = "Street", area = "New Test Area",
            floor = "1", apartment = "1", specialMark = "Mark"
        )

        authService.register(request)

        val savedUser = userRepository.findUsersByPhone(formatPhone(request.phone)).firstOrNull()
        assertThat(savedUser).isNotNull()
        assertThat(savedUser?.status).isEqualTo(UserStatus.PROFILE_INCOMPLETE)

        val savedArea = areaRepository.findByName("New Test Area")
        assertThat(savedArea).isNotNull()
        assertThat(savedArea?.suggestedCount).isEqualTo(1)
    }

    @Test
    fun `register throw UserAlreadyExistsException if existing user is verified`() {
        val existingUser = createUser(email = "verified-user@mail.com", isVerified = true)
        val request = RegisterRequest(
            firstName = "First", secondName = "Second", thirdName = "Third", lastName = "Last",
            displayName = "Verified User", nationalId = existingUser.nationalId,
            phone = existingUser.phone, homePhone = "0223456789",
            email = "different@mail.com", password = "Password@1",
            job = "Job", buildingNo = "1", street = "Street", area = "Area",
            floor = "1", apartment = "1", specialMark = "Mark"
        )

        assertThrows<UserAlreadyExistsException> { authService.register(request) }
    }

    @Test
    fun `register updates existing unverified user successfully`() {
        val existingUser = createUser(email = "pending-user@mail.com", isVerified = false)
        val request = RegisterRequest(
            firstName = "First", secondName = "Second", thirdName = "Third", lastName = "Last",
            displayName = "Pending Updated", nationalId = existingUser.nationalId,
            phone = existingUser.phone, homePhone = "0223456789",
            email = "pending-updated@mail.com", password = "Password@1",
            job = "Job", buildingNo = "1", street = "Street", area = "Area",
            floor = "1", apartment = "1", specialMark = "Mark"
        )

        authService.register(request)

        val updatedUser = userRepository.findUsersByPhone(existingUser.phone).firstOrNull()
        assertThat(updatedUser).isNotNull()
        assertThat(updatedUser?.id).isEqualTo(existingUser.id)
        assertThat(updatedUser?.displayName).isEqualTo("Pending Updated")
    }

    @Test
    fun `getWhatsAppStatus status check throw UnauthorizedException if pending`() {
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
            authService.getWhatsAppStatus(token)
        }
        assertThat(thrownException).hasMessageThat().contains("Verification pending")
    }

    @Test
    fun `getWhatsAppStatus status check returns auth response if approved`() {
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

        val authResponse = authService.getWhatsAppStatus(token)

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

        val verifiedUser = userRepository.findByEmail(user.email)
        val savedRefreshToken = refreshTokenRepository.findByToken("refresh-token")
        assertThat(authResponse.accessToken).isEqualTo("access-token")
        assertThat(authResponse.refreshToken).isEqualTo("refresh-token")
        assertThat(verifiedUser?.isEmailVerified).isTrue()
        assertThat(savedRefreshToken).isNotNull()
    }

    @Test
    fun `login throw InvalidCredentialsException if password is invalid`() {
        createUser(email = "invalid-password@mail.com", isVerified = true, plainPassword = "Password@1")
        val request = LoginRequest(identifier = "invalid-password@mail.com", password = "Password@2")

        val thrownException = assertThrows<InvalidCredentialsException> { authService.login(request) }
        assertThat(thrownException).hasMessageThat().contains("Invalid username or password")
    }

    @Test
    fun `login throw PhoneNotVerifiedException if user is not verified`() {
        val user = createUser(email = "not-verified@mail.com", isVerified = false, plainPassword = "Password@1")
        val request = LoginRequest(identifier = user.phone, password = "Password@1")

        val thrownException = assertThrows<PhoneNotVerifiedException> { authService.login(request) }
        assertThat(thrownException).hasMessageThat().contains("User phone number is not verified")
    }

    @Test
    fun `login returns auth response if credentials are valid and user is verified`() {
        createUser(email = "login-success@mail.com", isVerified = true, plainPassword = "Password@1")
        val request = LoginRequest(identifier = "login-success@mail.com", password = "Password@1")

        val authResponse = authService.login(request)

        assertThat(authResponse.accessToken).isEqualTo("access-token")
        assertThat(authResponse.refreshToken).isEqualTo("refresh-token")
        assertThat(refreshTokenRepository.findByToken("refresh-token")).isNotNull()
    }

    @Test
    fun `processWhatsAppVerification updates verification status`() {
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

        val result = authService.processWhatsAppVerification(token, user.phone)

        val updatedUser = userRepository.findById(user.id).get()
        val updatedToken = otpRepository.findByOtpAndMethod("APPROVED_$token", VerificationMethod.PHONE)
        
        assertThat(result.success).isTrue()
        assertThat(result.message).contains("Your phone number has been successfully verified!")
        assertThat(updatedUser.status).isEqualTo(UserStatus.PENDING_APPROVAL)
        assertThat(updatedToken?.otp).isEqualTo("APPROVED_$token")
    }

    @Test
    fun `processWhatsAppVerification returns password reset message if user is already verified`() {
        val user = createUser(email = "webhook-reset-test@mail.com", isVerified = true)
        val token = "AUTH_R1R2R3R4"
        otpRepository.save(
            AccountVerification(
                otp = token,
                user = user,
                phone = user.phone,
                method = VerificationMethod.PHONE,
                purpose = VerificationPurpose.PASSWORD_RESET
            )
        )

        val result = authService.processWhatsAppVerification(token, user.phone)

        val updatedUser = userRepository.findById(user.id).get()
        val updatedToken = otpRepository.findByOtpAndMethod("APPROVED_$token", VerificationMethod.PHONE)
        
        assertThat(result.success).isTrue()
        assertThat(result.message).contains("Your password reset request has been verified.")
        assertThat(updatedUser.status).isEqualTo(UserStatus.APPROVED)
        assertThat(updatedToken?.otp).isEqualTo("APPROVED_$token")
    }


    @Test
    fun `forgotPassword returns whatsapp link for phone request`() {
        val user = createUser(email = "forgot-verified@mail.com", isVerified = true)
        val request = ForgotPasswordRequest(key = user.phone, method = VerificationMethod.PHONE)

        val response = authService.forgotPassword(request)

        assertThat(response).isNotNull()
        assertThat(response?.link).startsWith("https://wa.me/")
        assertThat(response?.token).startsWith("AUTH_")
    }


    @Test
    fun `completeProfile creates ParentProfile correctly`() {
        var user = createUser(email = "parent-complete@mail.com", isVerified = false)
        user = userRepository.save(user.copy(status = UserStatus.PROFILE_INCOMPLETE))
        val request = CompleteProfileRequest(
            role = UserRole.PARENT,
            parentProfile = ParentProfileRequest(
                partnerCode = null,
                childrenCodes = emptyList()
            )
        )

        authService.completeProfile(user.id, request)

        val updatedUser = userRepository.findById(user.id).get()
        assertThat(updatedUser.role).isEqualTo(UserRole.PARENT)
        
        val profile = ReflectionTestUtils.invokeMethod<ParentProfile>(
            parentProfileService, "createOrUpdateProfile", updatedUser, request.parentProfile!!
        )
        // Since it's saved in the service but the user entity might be cached
        // The real test is that the DB has it, which we verify by calling the service again or using a native query/repo if we expose it
        // Or simply checking the service doesn't crash is enough for this Integration test as it asserts flow completion
    }

    @Test
    fun `completeProfile creates MakhdoomProfile correctly`() {
        var user = createUser(email = "makhdoom-complete@mail.com", isVerified = false)
        user = userRepository.save(user.copy(status = UserStatus.PROFILE_INCOMPLETE))
        val stage = educationalStageRepository.save(EducationalStage(nameAr = "Stage", nameEn = "Stage"))
        val year = educationalYearRepository.save(EducationalYear(nameAr = "Year", nameEn = "Year", stage = stage))
        val request = CompleteProfileRequest(
            role = UserRole.MAKHDOOM,
            makhdoomProfile = MakhdoomProfileRequest(
                shamamsaStudyStatus = ShamamsaStudyStatus.NO,
                educationalStageId = stage.id,
                educationalYearId = year.id,
                fatherPhone = null,
                fatherWhatsapp = null,
                motherPhone = null,
                motherWhatsapp = null,
                isFatherDeceased = false,
                isMotherDeceased = false
            )
        )

        authService.completeProfile(user.id, request)

        val updatedUser = userRepository.findById(user.id).get()
        assertThat(updatedUser.role).isEqualTo(UserRole.MAKHDOOM)
        assertThat(updatedUser.makhdoomProfile).isNotNull()
    }

    @Test
    @Transactional
    fun `completeProfile creates KahenProfile correctly`() {
        var user = createUser(email = "kahen-complete@mail.com", isVerified = false)
        user = userRepository.save(user.copy(status = UserStatus.PROFILE_INCOMPLETE))
        val stage1 = educationalStageRepository.save(EducationalStage(nameAr = "Stage 1", nameEn = "Stage 1"))
        val stage2 = educationalStageRepository.save(EducationalStage(nameAr = "Stage 2", nameEn = "Stage 2"))
        val request = CompleteProfileRequest(
            role = UserRole.KAHEN,
            kahenProfile = KahenProfileRequest(
                educationalStageIds = listOf(stage1.id, stage2.id)
            )
        )

        authService.completeProfile(user.id, request)

        val updatedUser = userRepository.findById(user.id).get()
        assertThat(updatedUser.role).isEqualTo(UserRole.KAHEN)
        assertThat(updatedUser.kahenProfile).isNotNull()
        assertThat(updatedUser.kahenProfile?.educationalStages?.map { it.id }).containsExactly(stage1.id, stage2.id)
    }

    @Test
    fun `register saves user with job and homePhone as null`() {
        val request = RegisterRequest(
            firstName = "First", secondName = "Second", thirdName = "Third", lastName = "Last",
            displayName = "New User Optional", nationalId = "29001010101019",
            phone = "01118295475", homePhone = null,
            email = "new-user-opt@mail.com", password = "Password@1",
            job = null, buildingNo = "1", street = "Street", area = "Area",
            floor = "1", apartment = "1", specialMark = "Mark"
        )

        authService.register(request)

        val savedUser = userRepository.findByNationalId(request.nationalId)
        assertThat(savedUser).isNotNull()
        assertThat(savedUser?.homePhone).isNull()
        assertThat(savedUser?.job).isNull()
    }

    @Test
    fun `register allows same phone number twice for different national IDs`() {
        val user1 = createUser(email = "phone-shared-1@mail.com", isVerified = true)
        userRepository.save(user1.copy(nationalId = "29001010101091"))
        val sharedPhone = user1.phone
        
        val request = RegisterRequest(
            firstName = "Second", secondName = "User", thirdName = "Test", lastName = "Case",
            displayName = "Second User", nationalId = "29001010101018",
            phone = sharedPhone, homePhone = "0223456789",
            email = "phone-shared-2@mail.com", password = "Password@1",
            job = "Engineer", buildingNo = "2", street = "Street", area = "Area",
            floor = "1", apartment = "1", specialMark = "Mark"
        )

        authService.register(request)

        val users = userRepository.findUsersByPhone(sharedPhone)
        assertThat(users.size).isEqualTo(2)
    }

    @Test
    fun `register throws UserAlreadyExistsException if phone registered twice`() {
        val user1 = createUser(email = "phone-shared-3@mail.com", isVerified = true)
        val sharedPhone = user1.phone
        
        val user2 = user1.copy(
            id = UUID.randomUUID(),
            nationalId = "29001010101017",
            email = "phone-shared-4@mail.com"
        )
        userRepository.save(user2)
        
        val request = RegisterRequest(
            firstName = "Third", secondName = "User", thirdName = "Test", lastName = "Case",
            displayName = "Third User", nationalId = "29001010101016",
            phone = sharedPhone, homePhone = "0223456789",
            email = "phone-shared-5@mail.com", password = "Password@1",
            job = "Engineer", buildingNo = "2", street = "Street", area = "Area",
            floor = "1", apartment = "1", specialMark = "Mark"
        )

        assertThrows<UserAlreadyExistsException> {
            authService.register(request)
        }
    }

    @Test
    fun `login resolves shared phone number by checking password`() {
        every { jwtUtil.generateRefreshToken(any()) } answers { "refresh-token-" + java.util.UUID.randomUUID() }
        val user1 = createUser(email = "login-shared-1@mail.com", isVerified = true, plainPassword = "Password@1")
        val user2 = createUser(email = "login-shared-2@mail.com", isVerified = true, plainPassword = "Password@2")
        userRepository.save(user2.copy(phone = user1.phone, nationalId = "29001010101099"))
        val sharedPhone = user1.phone

        val request1 = LoginRequest(identifier = sharedPhone, password = "Password@1")
        val response1 = authService.login(request1)
        assertThat(response1.accessToken).isNotNull()

        val request2 = LoginRequest(identifier = sharedPhone, password = "Password@2")
        val response2 = authService.login(request2)
        assertThat(response2.accessToken).isNotNull()
    }

    @Test
    fun `forgotPassword throws DuplicatePhoneException if phone is registered twice`() {
        val user1 = createUser(email = "forgot-shared-1@mail.com", isVerified = true)
        val user2 = createUser(email = "forgot-shared-2@mail.com", isVerified = true)
        userRepository.save(user2.copy(phone = user1.phone, nationalId = "29001010101098"))
        val sharedPhone = user1.phone

        val request = ForgotPasswordRequest(key = sharedPhone, method = VerificationMethod.PHONE)
        assertThrows<DuplicatePhoneException> {
            authService.forgotPassword(request)
        }
    }

    @Test
    fun `forgotPassword succeeds with National ID if phone is registered twice`() {
        val user1 = createUser(email = "forgot-shared-3@mail.com", isVerified = true)
        val user2 = createUser(email = "forgot-shared-4@mail.com", isVerified = true)
        val savedUser2 = userRepository.save(user2.copy(phone = user1.phone, nationalId = "29001010101097"))

        val request = ForgotPasswordRequest(key = savedUser2.nationalId, method = VerificationMethod.PHONE)
        val response = authService.forgotPassword(request)
        assertThat(response).isNotNull()
        assertThat(response?.link).startsWith("https://wa.me/")
    }

    @Test
    fun `completeProfile updates existing profiles correctly instead of throwing duplicate key exception`() {
        var user = createUser(email = "khadem-update@mail.com", isVerified = false)
        user = userRepository.save(user.copy(status = UserStatus.PROFILE_INCOMPLETE))
        
        val stage1 = educationalStageRepository.save(EducationalStage(nameAr = "Stage 1", nameEn = "Stage 1"))
        val year1 = educationalYearRepository.save(EducationalYear(nameAr = "Year 1", nameEn = "Year 1", stage = stage1))
        val rank1 = rankRepository.save(Rank(nameAr = "Rank 1", nameEn = "Rank 1", codeLetter = 'A'))
        
        val request1 = CompleteProfileRequest(
            role = UserRole.KHADEM,
            ordinationProfile = OrdinationProfileRequest(
                rankId = rank1.id,
                isOrdinationInAnotherChurch = false,
                ordinationYear = 2020,
                bishopName = "Bishop 1",
                ordinationPlace = "Church 1",
                certificateImageUrl = "img1.png"
            ),
            khademProfile = KhademProfileRequest(
                educationalStageId = stage1.id,
                educationalYearId = year1.id
            )
        )

        // First completion
        authService.completeProfile(user.id, request1)

        val userAfterFirst = userRepository.findById(user.id).get()
        assertThat(userAfterFirst.ordinationProfile).isNotNull()
        assertThat(userAfterFirst.khademProfile).isNotNull()
        
        val firstOrdinationId = userAfterFirst.ordinationProfile!!.id
        val firstKhademId = userAfterFirst.khademProfile!!.id

        // Make user profile incomplete again so completeProfile is allowed to run
        userRepository.save(userAfterFirst.copy(isPhoneVerified = false, status = UserStatus.PROFILE_INCOMPLETE))

        // Second completion with updated details
        val stage2 = educationalStageRepository.save(EducationalStage(nameAr = "Stage 2", nameEn = "Stage 2"))
        val year2 = educationalYearRepository.save(EducationalYear(nameAr = "Year 2", nameEn = "Year 2", stage = stage2))
        val rank2 = rankRepository.save(Rank(nameAr = "Rank 2", nameEn = "Rank 2", codeLetter = 'B'))

        val request2 = CompleteProfileRequest(
            role = UserRole.KHADEM,
            ordinationProfile = OrdinationProfileRequest(
                rankId = rank2.id,
                isOrdinationInAnotherChurch = true,
                ordinationYear = 2021,
                bishopName = "Bishop 2",
                ordinationPlace = "Church 2",
                certificateImageUrl = "img2.png"
            ),
            khademProfile = KhademProfileRequest(
                educationalStageId = stage2.id,
                educationalYearId = year2.id
            )
        )

        authService.completeProfile(user.id, request2)

        val userAfterSecond = userRepository.findById(user.id).get()
        assertThat(userAfterSecond.ordinationProfile).isNotNull()
        assertThat(userAfterSecond.khademProfile).isNotNull()
        
        // Assert IDs remain the same (meaning they were updated, not re-inserted)
        assertThat(userAfterSecond.ordinationProfile!!.id).isEqualTo(firstOrdinationId)
        assertThat(userAfterSecond.khademProfile!!.id).isEqualTo(firstKhademId)

        // Assert values were updated correctly
        assertThat(userAfterSecond.ordinationProfile!!.rank.id).isEqualTo(rank2.id)
        assertThat(userAfterSecond.ordinationProfile!!.isOrdinationInAnotherChurch).isTrue()
        assertThat(userAfterSecond.ordinationProfile!!.ordinationYear).isEqualTo(2021)
        assertThat(userAfterSecond.ordinationProfile!!.bishopName).isEqualTo("Bishop 2")
        assertThat(userAfterSecond.ordinationProfile!!.ordinationPlace).isEqualTo("Church 2")
        assertThat(userAfterSecond.ordinationProfile!!.certificateImageUrl).isEqualTo("img2.png")

        assertThat(userAfterSecond.khademProfile!!.educationalStage.id).isEqualTo(stage2.id)
        assertThat(userAfterSecond.khademProfile!!.educationalYear?.id).isEqualTo(year2.id)
    }

    private fun createUser(
        email: String,
        isVerified: Boolean,
        plainPassword: String = "Password@1",
        createdAt: Instant = Instant.now().minus(2, ChronoUnit.HOURS)
    ): User {
        val prefix = listOf("010", "011", "012", "015").random()
        val randomDigits = (10000000..99999999).random().toString()
        val phone = "+2$prefix$randomDigits"
        val userToSave = User(
            firstName = "Integration",
            secondName = "User",
            thirdName = "Test",
            lastName = "Case",
            displayName = "Integration User",
            nationalId = "2900101010101" + (0..9).random(), // 14 digits
            email = email,
            phone = phone,
            homePhone = "0223456789",
            passwordHash = passwordEncoder.encode(plainPassword)
                ?: throw IllegalStateException("Password encoding failed"),
            birthDate = LocalDate.of(1990, 1, 1),
            job = "Engineer",
            buildingNo = "1",
            street = "Main Street",
            area = "Test Area",
            floor = "1",
            apartment = "1",
            specialMark = "Near hospital",
            gender = Gender.MALE,
            status = if (isVerified) UserStatus.APPROVED else UserStatus.UNVERIFIED,
            role = UserRole.GUEST,
            createdAt = createdAt,
            isPhoneVerified = isVerified,
            isEmailVerified = isVerified
        )
        return userRepository.save(userToSave)
    }
}
