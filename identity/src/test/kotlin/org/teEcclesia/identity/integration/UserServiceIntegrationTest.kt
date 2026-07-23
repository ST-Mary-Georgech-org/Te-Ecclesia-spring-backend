package org.teEcclesia.identity.integration

import com.google.common.truth.Truth.assertThat
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
import org.teEcclesia.identity.entity.VerificationPurpose
import org.teEcclesia.identity.entity.VerificationMethod
import org.teEcclesia.identity.entity.lookups.EducationalStage
import org.teEcclesia.identity.entity.lookups.EducationalYear
import org.teEcclesia.identity.entity.enums.Gender
import org.teEcclesia.identity.entity.enums.UserStatus
import org.teEcclesia.identity.entity.enums.UserRole
import org.teEcclesia.identity.exception.UserNotFoundException
import org.teEcclesia.identity.repository.*
import org.teEcclesia.identity.service.UserService
import org.teEcclesia.storage.service.ImageStorageService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
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
    private lateinit var imageStorageService: ImageStorageService

    @Autowired
    private lateinit var teEcclesiaEventPublisher: TeEcclesiaEventPublisher

    @BeforeEach
    fun setUp() {
        refreshTokenRepository.deleteAll()
        emailVerificationRepository.deleteAll()
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

    private fun createUser(email: String, imageUrl: String? = null): User {
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
                role = UserRole.GUEST,
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
        val year = educationalYearRepository.save(EducationalYear(nameAr = "Year", nameEn = "Year", stage = stage))

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
        val user = createUser(email = "pending-search-null@mail.com")
        userRepository.save(user.copy(status = UserStatus.PENDING_APPROVAL))

        val page = userService.getUsersByStatus(
            status = UserStatus.PENDING_APPROVAL,
            stageId = null,
            yearId = null,
            role = null,
            search = null,
            pageable = org.springframework.data.domain.PageRequest.of(0, 20)
        )

        assertThat(page.content).isNotEmpty()
    }
}

