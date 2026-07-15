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
import org.teEcclesia.identity.entity.User
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
    private lateinit var imageStorageService: ImageStorageService

    @Autowired
    private lateinit var teEcclesiaEventPublisher: TeEcclesiaEventPublisher

    @BeforeEach
    fun setUp() {
        refreshTokenRepository.deleteAll()
        emailVerificationRepository.deleteAll()
        userRepository.deleteAll()
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
                phone = "123456_" + UUID.randomUUID().toString().take(6),
                homePhone = "0223456789",
                passwordHash = "encoded-password",
                birthDate = LocalDate.of(1990, 1, 1),
                job = "Engineer",
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
            phone = "987654321",
            email = "new-email@mail.com"
        )

        userService.updateProfile(existingUser.id, request)

        val updatedUser = userRepository.findById(existingUser.id).orElse(null)
        assertThat(updatedUser).isNotNull()
        assertThat(updatedUser?.displayName).isEqualTo("Israa Updated")
        assertThat(updatedUser?.phone).isEqualTo("987654321")
        assertThat(updatedUser?.email).isEqualTo("new-email@mail.com")
    }
}
