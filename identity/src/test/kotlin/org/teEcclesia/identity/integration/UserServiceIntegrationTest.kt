package org.teEcclesia.identity.integration

import com.google.common.truth.Truth.assertThat
import io.mockk.called
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.teEcclesia.events.publisher.TeEcclesiaEventPublisher
import org.teEcclesia.identity.IdentityTestApplication
import org.teEcclesia.identity.api.dto.request.UpdateProfileRequest
import org.teEcclesia.identity.api.dto.request.RegisterRequest
import org.teEcclesia.identity.api.dto.request.MakhdoomProfileRequest
import org.teEcclesia.identity.entity.User
import org.teEcclesia.identity.entity.ParentProfile
import org.teEcclesia.identity.entity.VerificationPurpose
import org.teEcclesia.identity.entity.VerificationMethod
import org.teEcclesia.identity.entity.lookups.EducationalStage
import org.teEcclesia.identity.entity.lookups.EducationalYear
import org.teEcclesia.identity.entity.enums.Gender
import org.teEcclesia.identity.entity.enums.UserStatus
import org.teEcclesia.identity.entity.enums.UserRole
import org.teEcclesia.identity.exception.UserNotFoundException
import org.teEcclesia.identity.exception.UserAlreadyExistsException
import org.teEcclesia.identity.repository.*
import org.teEcclesia.identity.api.dto.request.ApproveUserRequest
import org.teEcclesia.identity.api.dto.request.OrdinationProfileRequest
import org.teEcclesia.identity.entity.lookups.Rank
import org.teEcclesia.identity.service.UserService
import org.teEcclesia.identity.service.SystemSettingService
import org.teEcclesia.identity.api.dto.request.DeaconsSchoolRecordRequest
import org.teEcclesia.identity.entity.enums.DeaconsSchoolStatus
import java.math.BigDecimal
import org.teEcclesia.storage.service.ImageStorageService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import org.teEcclesia.identity.entity.enums.ShamamsaStudyStatus
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*

@SpringBootTest(classes = [IdentityTestApplication::class])
@ActiveProfiles("test")
class UserServiceIntegrationTest {

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @Autowired
    private lateinit var emailVerificationRepository: EmailVerificationRepository

    @Autowired
    private lateinit var educationalStageRepository: EducationalStageRepository

    @Autowired
    private lateinit var educationalYearRepository: EducationalYearRepository

    @Autowired
    private lateinit var rankRepository: RankRepository

    @Autowired
    private lateinit var parentProfileRepository: ParentProfileRepository

    @Autowired
    private lateinit var imageStorageService: ImageStorageService

    @Autowired
    private lateinit var teEcclesiaEventPublisher: TeEcclesiaEventPublisher

    @Autowired
    private lateinit var deaconsSchoolRecordRepository: DeaconsSchoolRecordRepository

    @Autowired
    private lateinit var systemSettingService: SystemSettingService

    @BeforeEach
    fun setUp() {
        refreshTokenRepository.deleteAll()
        emailVerificationRepository.deleteAll()
        parentProfileRepository.deleteAll()
        userRepository.deleteAll()
        educationalYearRepository.deleteAll()
        educationalStageRepository.deleteAll()
        clearMocks(imageStorageService, answers = false, recordedCalls = true)
    }

    @Test
    fun `existById returns true if user exists`() {
        val existingUser = createUser(email = "exist-true@mail.com")

        val userExists = userService.existById(existingUser.id)

        assertThat(userExists).isTrue()
    }

    @Test
    fun `existById returns false if user does not exist`() {
        val missingUserId = UUID.randomUUID()

        val userExists = userService.existById(missingUserId)

        assertThat(userExists).isFalse()
    }

    @Test
    fun `findById returns user if user exists`() {
        val existingUser = createUser(email = "find-user@mail.com")

        val foundUser = userService.findById(existingUser.id)

        assertThat(foundUser.id).isEqualTo(existingUser.id)
        assertThat(foundUser.email).isEqualTo(existingUser.email)
    }

    @Test
    fun `findById throw UserNotFoundException if user does not exist`() {
        val missingUserId = UUID.randomUUID()

        val thrownException = assertThrows<UserNotFoundException> { userService.findById(missingUserId) }

        assertThat(thrownException).hasMessageThat().contains("User with id: $missingUserId not found")
    }

    @Test
    fun `updateUserImage returns uploaded image url if user exists`() {
        val existingUser = createUser(email = "image-update@mail.com")
        every {
            imageStorageService.uploadImage(any(), eq(existingUser.id.toString()), eq("test-profile-images"))
        } returns "updated-image.jpg?time=test"
        val imageFile = MockMultipartFile("file", "profile.jpg", "image/jpeg", "file-content".toByteArray())

        val returnedImageUrl = userService.updateUserImage(existingUser.id, imageFile)

        val updatedUser = userRepository.findByEmail(existingUser.email!!)
        assertThat(returnedImageUrl).isEqualTo("updated-image.jpg?time=test")
        assertThat(updatedUser?.imageUrl).isEqualTo("updated-image.jpg?time=test")
    }

    @Test
    fun `updateUserImage throw UserNotFoundException if user does not exist`() {
        val missingUserId = UUID.randomUUID()
        val imageFile = MockMultipartFile("file", "profile.jpg", "image/jpeg", "file-content".toByteArray())

        val thrownException =
            assertThrows<UserNotFoundException> { userService.updateUserImage(missingUserId, imageFile) }

        assertThat(thrownException).hasMessageThat().contains("User with id: $missingUserId not found")
    }

    @Test
    fun `deleteUserImage returns by removing image if user has image`() {
        val existingUser = createUser(email = "delete-image@mail.com", imageUrl = "existing-image.jpg?time=test")

        userService.deleteUserImage(existingUser.id)

        val updatedUser = userRepository.findByEmail(existingUser.email!!)
        assertThat(updatedUser?.imageUrl).isNull()
        verify(exactly = 1) {
            imageStorageService.deleteImage(any(), "existing-image.jpg")
        }
    }

    @Test
    fun `deleteUserImage returns without changes if user has no image`() {
        val existingUser = createUser(email = "delete-no-image@mail.com", imageUrl = null)

        userService.deleteUserImage(existingUser.id)

        val userAfterDelete = userRepository.findByEmail(existingUser.email!!)
        assertThat(userAfterDelete?.imageUrl).isNull()
        verify(exactly = 0) {
            imageStorageService.deleteImage(any(), any())
        }
    }

    @Test
    fun `deleteUserImage throw UserNotFoundException if user does not exist`() {
        val missingUserId = UUID.randomUUID()

        val thrownException = assertThrows<UserNotFoundException> { userService.deleteUserImage(missingUserId) }

        assertThat(thrownException).hasMessageThat().contains("User with id: $missingUserId not found")
    }

    private fun createUser(email: String, imageUrl: String? = null, role: UserRole = UserRole.GUEST): User {
        val username = email.substringBefore("@")
        return userRepository.save(
            User(
                firstName = "Integration",
                secondName = "User",
                thirdName = "Test",
                lastName = "Case",
                displayName = "Integration User",
                nationalId = "2900101010101" + (0..9).random(), // 14 digits
                email = email,
                phone = "+2" + listOf("010", "011", "012", "015").random() + (10000000..99999999).random().toString(),
                homePhone = "0223456789",
                passwordHash = "encoded-password",
                birthDate = LocalDate.of(1990, 1, 1),
                job = "Engineer",
                buildingNo = "1",
                street = "Main Street",
                area = "Test Area",
                floor = "1",
                apartment = "1",
                specialMark = "Near hospital",
                gender = Gender.MALE,
                status = UserStatus.APPROVED,
                role = role,
                createdAt = Instant.now().minus(2, ChronoUnit.DAYS),
                imageUrl = imageUrl
            )
        )
    }

    @Test
    fun `updateProfile updates user data successfully if user exists`() {
        val existingUser = createUser(email = "profile-update@mail.com")
        val request = UpdateProfileRequest(
            firstName = "Israa",
            secondName = "Updated",
            thirdName = "Test",
            lastName = "Case",
            displayName = "Israa Updated",
            phone = "01118295476",
            email = "new-email@mail.com"
        )

        userService.updateProfile(existingUser.id, request)

        val updatedUser = userRepository.findById(existingUser.id).orElse(null)
        assertThat(updatedUser).isNotNull()
        assertThat(updatedUser?.displayName).isEqualTo("Israa Updated")
        assertThat(updatedUser?.phone).isEqualTo(existingUser.phone)
        assertThat(updatedUser?.email).isEqualTo("new-email@mail.com")
    }

    @Test
    fun `initiatePhoneChange successfully creates phone change verification token`() {
        val user = createUser(email = "phone-change-init@mail.com")
        val newPhone = "01118295477"
        val response = userService.initiatePhoneChange(user.id, newPhone)
        
        assertThat(response).isNotNull()
        assertThat(response.deepLink).startsWith("https://wa.me/")
        assertThat(response.token).startsWith("AUTH_")

        val tokenEntity = emailVerificationRepository.findByOtpAndMethod(response.token, VerificationMethod.PHONE)
        assertThat(tokenEntity).isNotNull()
        assertThat(tokenEntity?.phone).isEqualTo("+201118295477")
        assertThat(tokenEntity?.purpose).isEqualTo(VerificationPurpose.PHONE_CHANGE)
    }

    @Test
    fun `updateMakhdoomProfileByKhadem updates Makhdoom profile successfully`() {
        val admin = createUser(email = "admin-update@mail.com")
        userRepository.save(admin.copy(role = UserRole.ADMIN))
        
        val makhdoom = createUser(email = "makhdoom-update@mail.com")
        userRepository.save(makhdoom.copy(role = UserRole.MAKHDOOM))

        val stage = educationalStageRepository.save(EducationalStage(nameAr = "Stage", nameEn = "Stage"))
        val year = educationalYearRepository.save(EducationalYear(nameAr = "Year", nameEn = "Year", stage = stage, whatsAppLink = null))

        val request = RegisterRequest(
            firstName = "Makhdoom", secondName = "Updated", thirdName = "By", lastName = "Admin",
            displayName = "Makhdoom Updated", nationalId = "29001010101098",
            phone = "01118295479", homePhone = "0223456789",
            email = "makhdoom-new@mail.com", password = "NewPassword@1",
            job = "Student", buildingNo = "3", street = "Street", area = "Area",
            floor = "1", apartment = "1", specialMark = "Mark",
            role = UserRole.MAKHDOOM,
            makhdoomProfile = MakhdoomProfileRequest(
                shamamsaStudyStatus = ShamamsaStudyStatus.YES,
                educationalStageId = stage.id,
                educationalYearId = year.id,
                fatherPhone = "01118295470",
                fatherWhatsapp = "01118295470",
                motherPhone = "01118295471",
                motherWhatsapp = "01118295471",
                isFatherDeceased = false,
                isMotherDeceased = false
            )
        )

        userService.updateMakhdoomProfileByKhadem(admin.id, makhdoom.id, request)

        val updated = userRepository.findById(makhdoom.id).get()
        assertThat(updated.displayName).isEqualTo("Makhdoom Updated")
        assertThat(updated.phone).isEqualTo("+201118295479")
        assertThat(updated.makhdoomProfile).isNotNull()
        assertThat(updated.makhdoomProfile?.educationalStage?.id).isEqualTo(stage.id)
    }

    @Test
    fun `getUsersByStatus with null search executes without error`() {
        val user = createUser(email = "pending-search-null@mail.com", role = UserRole.ADMIN)
        userRepository.save(user.copy(status = UserStatus.PENDING_APPROVAL))

        val page = userService.getUsersByStatus(
            callerId = user.id,
            status = UserStatus.PENDING_APPROVAL,
            stageId = null,
            yearId = null,
            role = null,
            search = null,
            pageable = PageRequest.of(0, 20)
        )

        assertThat(page.content).isNotEmpty()
    }

    @Test
    fun `getUserProfile executes without error and returns ProfileResponse`() {
        val user = createUser(email = "get-user-profile@mail.com")

        val profile = userService.getUserProfile(user.id, "http://cdn.test")

        assertThat(profile).isNotNull()
        assertThat(profile.id).isEqualTo(user.id.toString())
    }

    @Test
    fun `createMakhdoomDirectly throws UserAlreadyExistsException when email is already approved and verified`() {
        val admin = createUser(email = "admin-makhdoom-test@mail.com")
        userRepository.save(admin.copy(role = UserRole.ADMIN, status = UserStatus.APPROVED))

        val existingUser = createUser(email = "verified-makhdoom-email@mail.com")
        userRepository.save(existingUser.copy(status = UserStatus.APPROVED, isEmailVerified = true))

        val stage = educationalStageRepository.save(EducationalStage(nameAr = "Stage", nameEn = "Stage"))

        val request = RegisterRequest(
            firstName = "Direct", secondName = "Makhdoom", thirdName = "Test", lastName = "Case",
            displayName = "Direct Makhdoom", nationalId = "29901010101099",
            phone = "01118295999", homePhone = "0223456789",
            email = "verified-makhdoom-email@mail.com", password = "Password@1",
            job = "Student", buildingNo = "1", street = "Street", area = "Area",
            floor = "1", apartment = "1", specialMark = "Mark",
            makhdoomProfile = MakhdoomProfileRequest(
                shamamsaStudyStatus = ShamamsaStudyStatus.NO,
                educationalStageId = stage.id,
                educationalYearId = null
            )
        )

        org.junit.jupiter.api.assertThrows<org.teEcclesia.identity.exception.UserAlreadyExistsException> {
            userService.createMakhdoomDirectly(admin.id, request)
        }
    }

    @Test
    fun `createMakhdoomDirectly succeeds when duplicate email belongs to an unverified non-approved user`() {
        val admin = createUser(email = "admin-makhdoom-test2@mail.com")
        userRepository.save(admin.copy(role = UserRole.ADMIN, status = UserStatus.APPROVED))

        val unverifiedUser = createUser(email = "unverified-shared-email@mail.com")
        userRepository.save(unverifiedUser.copy(status = UserStatus.UNVERIFIED, isEmailVerified = false))

        val stage = educationalStageRepository.save(EducationalStage(nameAr = "Stage", nameEn = "Stage"))

        val request = RegisterRequest(
            firstName = "Direct", secondName = "Makhdoom", thirdName = "Test", lastName = "Case",
            displayName = "Direct Makhdoom", nationalId = "29901010101088",
            phone = "01118295888", homePhone = "0223456789",
            email = "unverified-shared-email@mail.com", password = "Password@1",
            job = "Student", buildingNo = "1", street = "Street", area = "Area",
            floor = "1", apartment = "1", specialMark = "Mark",
            makhdoomProfile = MakhdoomProfileRequest(
                shamamsaStudyStatus = ShamamsaStudyStatus.NO,
                educationalStageId = stage.id,
                educationalYearId = null
            )
        )

        userService.createMakhdoomDirectly(admin.id, request)
        val created = userRepository.findUsersByEmail("unverified-shared-email@mail.com").lastOrNull()
        assertThat(created?.email).isEqualTo("unverified-shared-email@mail.com")
    }

    @Test
    fun `approveUser updates user profile data, role, and ordination profile correctly`() {
        val user = createUser(email = "unapproved-ordination-test@mail.com")
        userRepository.save(user.copy(status = UserStatus.PENDING_APPROVAL, role = UserRole.GUEST))

        val rank = rankRepository.save(Rank(nameAr = "Epsaltos", nameEn = "Epsaltos", codeLetter = 'A'))

        val updateRequest = RegisterRequest(
            firstName = "UpdatedFirst",
            secondName = user.secondName,
            thirdName = user.thirdName,
            lastName = user.lastName,
            displayName = "Updated Display Name",
            nationalId = user.nationalId,
            phone = user.phone,
            buildingNo = user.buildingNo,
            street = user.street,
            area = user.area,
            floor = user.floor,
            specialMark = user.specialMark,
            role = UserRole.MAKHDOOM,
            ordinationProfile = OrdinationProfileRequest(
                rankId = rank.id,
                isOrdinationInAnotherChurch = true,
                ordinationYear = 2022,
                bishopName = "Anba Thomas",
                ordinationPlace = "St. Mark Cathedral"
            )
        )

        val approveRequest = ApproveUserRequest(
            customCode = "A24991234",
            updateProfileData = updateRequest
        )

        userService.approveUser(user.id, approveRequest, callerId = null)

        val approvedUser = userRepository.findById(user.id).get()
        assertThat(approvedUser.status).isEqualTo(UserStatus.APPROVED)
        assertThat(approvedUser.firstName).isEqualTo("UpdatedFirst")
        assertThat(approvedUser.role).isEqualTo(UserRole.MAKHDOOM)
        assertThat(approvedUser.code).isEqualTo("A24991234")
        assertThat(approvedUser.ordinationProfile).isNotNull()
        assertThat(approvedUser.ordinationProfile?.ordinationPlace).isEqualTo("St. Mark Cathedral")
        assertThat(approvedUser.ordinationProfile?.bishopName).isEqualTo("Anba Thomas")
        assertThat(approvedUser.ordinationProfile?.ordinationYear).isEqualTo(2022)
    }

    @Test
    fun testApproveUser_WithExistingRejectedUserWithSameNationalIdAndPhone() {
        val nationalId = "29901011234567"
        val phone = "01012345678"

        // Create an existing REJECTED user with phone verified
        val baseRejected = createUser(email = "rejected@test.com")
        userRepository.save(
            baseRejected.copy(
                phone = phone,
                nationalId = nationalId,
                status = UserStatus.REJECTED,
                isPhoneVerified = true
            )
        )

        // Create a PENDING_APPROVAL user with same phone and nationalId
        val basePending = createUser(email = "pending@test.com")
        val pendingUser = userRepository.save(
            basePending.copy(
                phone = phone,
                nationalId = nationalId,
                status = UserStatus.PENDING_APPROVAL,
                isPhoneVerified = true
            )
        )

        val updateRequest = RegisterRequest(
            firstName = pendingUser.firstName,
            secondName = pendingUser.secondName,
            thirdName = pendingUser.thirdName,
            lastName = pendingUser.lastName,
            displayName = pendingUser.displayName,
            nationalId = nationalId,
            phone = phone,
            buildingNo = pendingUser.buildingNo,
            street = pendingUser.street,
            area = pendingUser.area,
            floor = pendingUser.floor,
            specialMark = pendingUser.specialMark,
            role = UserRole.MAKHDOOM
        )

        val approveRequest = ApproveUserRequest(
            updateProfileData = updateRequest
        )

        userService.approveUser(pendingUser.id, approveRequest, callerId = null)

        val approvedUser = userRepository.findById(pendingUser.id).get()
        assertThat(approvedUser.status).isEqualTo(UserStatus.APPROVED)
    }

    @Test
    fun testApproveUser_FailsWhenAnotherActiveUserHasSameNationalIdEvenWithDifferentPhone() {
        val nationalId = "29901019999999"
        val phone1 = "01011112222"
        val phone2 = "01033334444"

        // Create an existing APPROVED user with phone1
        val baseApproved = createUser(email = "approved-other-phone@test.com")
        userRepository.save(
            baseApproved.copy(
                phone = phone1,
                nationalId = nationalId,
                status = UserStatus.APPROVED,
                isPhoneVerified = true
            )
        )

        // Create a PENDING_APPROVAL user with phone2 and same nationalId
        val basePending = createUser(email = "pending-other-phone@test.com")
        val pendingUser = userRepository.save(
            basePending.copy(
                phone = phone2,
                nationalId = nationalId,
                status = UserStatus.PENDING_APPROVAL,
                isPhoneVerified = true
            )
        )

        val updateRequest = RegisterRequest(
            firstName = pendingUser.firstName,
            secondName = pendingUser.secondName,
            thirdName = pendingUser.thirdName,
            lastName = pendingUser.lastName,
            displayName = pendingUser.displayName,
            nationalId = nationalId,
            phone = phone2,
            buildingNo = pendingUser.buildingNo,
            street = pendingUser.street,
            area = pendingUser.area,
            floor = pendingUser.floor,
            specialMark = pendingUser.specialMark,
            role = UserRole.MAKHDOOM
        )
        val approveRequest = ApproveUserRequest(
            updateProfileData = updateRequest
        )

        assertThrows<RuntimeException> {
            userService.approveUser(pendingUser.id, approveRequest, callerId = null)
        }
    }

    @Test
    fun `toUserSummaryResponse correctly populates real fullName instead of displayName`() {
        val admin = userRepository.save(
            createUser(email = "admin@test.com", role = UserRole.ADMIN)
        )
        val priest = userRepository.save(
            createUser(email = "priest@test.com", role = UserRole.KAHEN).copy(
                displayName = "Abouna Mina",
                firstName = "Mina",
                secondName = "Adly",
                thirdName = "Naguib",
                lastName = "Bishoy"
            )
        )
        val child = userRepository.save(
            createUser(email = "child@test.com").copy(
                displayName = "Fady",
                firstName = "Fady",
                secondName = "Naguib",
                thirdName = "Kamel",
                lastName = "Boulos",
                confessionPriest = priest
            )
        )
        val parentUser = userRepository.save(
            createUser(email = "parent@test.com", role = UserRole.PARENT).copy(
                displayName = "Parent Nabil",
                firstName = "Nabil",
                secondName = "Kamel",
                thirdName = "Boulos",
                lastName = "Girgis"
            )
        )
        parentProfileRepository.save(
            ParentProfile(
                user = parentUser,
                children = listOf(child)
            )
        )
        val profiles = userService.getUsersByStatus(
            callerId = admin.id,
            status = UserStatus.APPROVED,
            stageId = null,
            yearId = null,
            role = null,
            search = null,
            pageable = PageRequest.of(0, 10)
        )
        val childProfile = profiles.content.find { it.id == child.id.toString() }
        assertThat(childProfile).isNotNull()
        assertThat(childProfile?.confessionPriest?.name).isEqualTo("Abouna Mina")
        assertThat(childProfile?.confessionPriest?.fullName).isEqualTo("Mina Adly Naguib Bishoy")

        val parentResponse = profiles.content.find { it.id == parentUser.id.toString() }
        assertThat(parentResponse).isNotNull()
        assertThat(parentResponse?.parentProfile?.children).hasSize(1)
        val childSummary = parentResponse?.parentProfile?.children?.first()
        assertThat(childSummary?.name).isEqualTo("Fady")
        assertThat(childSummary?.fullName).isEqualTo("Fady Naguib Kamel Boulos")
    }

    @Test
    fun `approveUser records actionTakenBy and actionTakenAt`() {
        val admin = createUser(email = "admin-action-test@mail.com")
        userRepository.save(admin.copy(role = UserRole.ADMIN, status = UserStatus.APPROVED))

        val pending = createUser(email = "pending-action-test@mail.com")
        userRepository.save(pending.copy(status = UserStatus.PENDING_APPROVAL))

        userService.approveUser(pending.id, null, callerId = admin.id)

        val approved = userRepository.findById(pending.id).get()
        assertThat(approved.status).isEqualTo(UserStatus.APPROVED)
        assertThat(approved.actionTakenAt).isNotNull()
        assertThat(approved.actionTakenBy?.id).isEqualTo(admin.id)

        val profile = userService.getUserProfile(pending.id)
        assertThat(profile.actionTakenAt).isNotNull()
        assertThat(profile.actionTakenBy?.id).isEqualTo(admin.id)
    }

    @Test
    fun `updateUserByAdminOrKhadem saves deaconsSchoolRecord for current academic year`() {
        val admin = createUser(email = "admin-deacon-test@mail.com")
        userRepository.save(admin.copy(role = UserRole.ADMIN, status = UserStatus.APPROVED))

        val makhdoom = createUser(email = "makhdoom-deacon-test@mail.com")
        userRepository.save(makhdoom.copy(role = UserRole.MAKHDOOM, status = UserStatus.APPROVED))

        systemSettingService.updateCurrentAcademicYear(2026)

        val updateRequest = ApproveUserRequest(
            deaconsSchoolRecord = DeaconsSchoolRecordRequest(
                enrolled = true,
                paid = true,
                paidAmount = BigDecimal("150.00"),
                status = DeaconsSchoolStatus.COMPLETED
            )
        )

        userService.updateUserByAdminOrKhadem(
            callerId = admin.id,
            targetUserId = makhdoom.id,
            request = updateRequest
        )

        val profile2026 = userService.getUserProfile(makhdoom.id)
        assertThat(profile2026.deaconsSchoolRecord).isNotNull()
        assertThat(profile2026.deaconsSchoolRecord?.academicYear).isEqualTo(2026)
        assertThat(profile2026.deaconsSchoolRecord?.enrolled).isTrue()
        assertThat(profile2026.deaconsSchoolRecord?.paid).isTrue()
        assertThat(profile2026.deaconsSchoolRecord?.paidAmount).isEqualTo(BigDecimal("150.00"))
        assertThat(profile2026.deaconsSchoolRecord?.status).isEqualTo(DeaconsSchoolStatus.COMPLETED)

        // Change year to 2027
        systemSettingService.updateCurrentAcademicYear(2027)

        val profile2027 = userService.getUserProfile(makhdoom.id)
        assertThat(profile2027.deaconsSchoolRecord).isNotNull()
        assertThat(profile2027.deaconsSchoolRecord?.academicYear).isEqualTo(2027)
        assertThat(profile2027.deaconsSchoolRecord?.enrolled).isFalse()
        assertThat(profile2027.deaconsSchoolRecord?.paid).isFalse()

        // Switch back to 2026
        systemSettingService.updateCurrentAcademicYear(2026)
        val profile2026Again = userService.getUserProfile(makhdoom.id)
        assertThat(profile2026Again.deaconsSchoolRecord?.enrolled).isTrue()
        assertThat(profile2026Again.deaconsSchoolRecord?.paidAmount).isEqualTo(BigDecimal("150.00"))
    }

    @Test
    fun `rejectUser and banUser record actionTakenBy and actionTakenAt`() {
        val admin = createUser(email = "admin-reject-test@mail.com")
        userRepository.save(admin.copy(role = UserRole.ADMIN, status = UserStatus.APPROVED))

        val pending = createUser(email = "pending-reject-test@mail.com")
        userRepository.save(pending.copy(status = UserStatus.PENDING_APPROVAL))

        userService.rejectUser(pending.id, reason = "Incomplete papers", callerId = admin.id)

        val rejected = userRepository.findById(pending.id).get()
        assertThat(rejected.status).isEqualTo(UserStatus.REJECTED)
        assertThat(rejected.actionTakenAt).isNotNull()
        assertThat(rejected.actionTakenBy?.id).isEqualTo(admin.id)

        userService.banUser(pending.id, reason = "Violation", callerId = admin.id)

        val banned = userRepository.findById(pending.id).get()
        assertThat(banned.status).isEqualTo(UserStatus.BANNED)
        assertThat(banned.actionTakenAt).isNotNull()
        assertThat(banned.actionTakenBy?.id).isEqualTo(admin.id)
    }
}


