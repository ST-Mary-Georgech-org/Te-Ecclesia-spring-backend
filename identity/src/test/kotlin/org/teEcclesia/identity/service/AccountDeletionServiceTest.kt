package org.teEcclesia.identity.service

import com.google.common.truth.Truth.assertThat
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.security.crypto.password.PasswordEncoder
import org.teEcclesia.events.notifications.UserNotificationsEvent
import org.teEcclesia.events.publisher.TeEcclesiaEventPublisher
import org.teEcclesia.identity.api.dto.request.DeleteAccountRequest
import org.teEcclesia.identity.api.dto.request.ReactivateAccountRequest
import org.teEcclesia.identity.entity.AccountDeletionRequest
import org.teEcclesia.identity.entity.User
import org.teEcclesia.identity.entity.enums.Gender
import org.teEcclesia.identity.entity.enums.UserRole
import org.teEcclesia.identity.entity.enums.UserStatus
import org.teEcclesia.identity.exception.IncorrectPasswordException
import org.teEcclesia.identity.exception.InvalidCredentialsException
import org.teEcclesia.identity.exception.UnauthorizedException
import org.teEcclesia.identity.repository.AccountDeletionRequestRepository
import org.teEcclesia.identity.repository.RefreshTokenRepository
import org.teEcclesia.identity.repository.UserRepository
import org.teEcclesia.identity.repository.projection.AccountDeletionRequestProjection
import org.teEcclesia.identity.repository.projection.DeletedUserCheckProjection
import org.teEcclesia.identity.repository.projection.UserAuthDetailsProjection
import org.teEcclesia.identity.security.JwtUtil
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class AccountDeletionServiceTest {

    private val userRepository: UserRepository = mockk()
    private val accountDeletionRequestRepository: AccountDeletionRequestRepository = mockk(relaxed = true)
    private val refreshTokenRepository: RefreshTokenRepository = mockk(relaxed = true)
    private val passwordEncoder: PasswordEncoder = mockk()
    private val jwtUtil: JwtUtil = mockk()
    private val eventPublisher: TeEcclesiaEventPublisher = mockk(relaxed = true)

    private lateinit var service: AccountDeletionService

    @BeforeEach
    fun setUp() {
        service = AccountDeletionService(
            userRepository = userRepository,
            accountDeletionRequestRepository = accountDeletionRequestRepository,
            refreshTokenRepository = refreshTokenRepository,
            passwordEncoder = passwordEncoder,
            jwtUtil = jwtUtil,
            eventPublisher = eventPublisher,
            cdnEndpoint = "http://localhost:8080",
            profileImageDirectory = "profiles"
        )
    }

    private fun buildSampleUser(
        id: UUID = UUID.randomUUID(),
        nationalId: String = "29001010101010",
        phone: String = "01000000000",
        passwordHash: String = "hashed_pw",
        role: UserRole = UserRole.MAKHDOOM,
        status: UserStatus = UserStatus.APPROVED,
        deleted: Boolean = false
    ): User {
        return User(
            id = id,
            firstName = "Mina",
            secondName = "George",
            thirdName = "Nabil",
            lastName = "Bishoy",
            displayName = "Mina",
            nationalId = nationalId,
            birthDate = LocalDate.of(1995, 1, 1),
            gender = Gender.MALE,
            phone = phone,
            passwordHash = passwordHash,
            buildingNo = "1",
            street = "Street",
            area = "Area",
            floor = "1",
            specialMark = "Mark",
            role = role,
            status = status,
            deleted = deleted
        )
    }

    private fun mockUserAuthDetails(
        userId: UUID,
        fullName: String = "Mina George",
        passwordHash: String = "hashed_pw",
        role: String = UserRole.MAKHDOOM.name,
        status: String = UserStatus.APPROVED.name
    ): UserAuthDetailsProjection {
        val proj = mockk<UserAuthDetailsProjection>()
        every { proj.getId() } returns userId.toString()
        every { proj.getFullName() } returns fullName
        every { proj.getPasswordHash() } returns passwordHash
        every { proj.getRole() } returns role
        every { proj.getStatus() } returns status
        return proj
    }

    private fun mockDeletedUserCheck(
        userId: UUID,
        nationalId: String = "29001010101010",
        fullName: String = "Mina George",
        passwordHash: String = "hashed_pw",
        status: UserStatus = UserStatus.APPROVED
    ): DeletedUserCheckProjection {
        val proj = mockk<DeletedUserCheckProjection>()
        every { proj.getId() } returns userId
        every { proj.getNationalId() } returns nationalId
        every { proj.getFullName() } returns fullName
        every { proj.getPasswordHash() } returns passwordHash
        every { proj.getStatus() } returns status
        return proj
    }

    private fun mockDeletionRequestProjection(
        requestId: UUID,
        userId: UUID,
        userName: String = "Mina George",
        userCode: String? = "M12345678",
        userImageUrl: String? = null,
        userRole: UserRole = UserRole.MAKHDOOM,
        reason: String = "Reason 1",
        requestedAt: Instant = Instant.now()
    ): AccountDeletionRequestProjection {
        val proj = mockk<AccountDeletionRequestProjection>()
        every { proj.getId() } returns requestId
        every { proj.getUserId() } returns userId
        every { proj.getUserName() } returns userName
        every { proj.getUserCode() } returns userCode
        every { proj.getUserImageUrl() } returns userImageUrl
        every { proj.getUserRole() } returns userRole
        every { proj.getReason() } returns reason
        every { proj.getRequestedAt() } returns requestedAt
        return proj
    }

    @Test
    fun `requestAccountDeletion throws IncorrectPasswordException when password does not match`() {
        val userId = UUID.randomUUID()
        val userAuth = mockUserAuthDetails(userId = userId)

        every { userRepository.findAuthDetailsById(userId) } returns userAuth
        every { passwordEncoder.matches("wrong_pw", any()) } returns false

        val request = DeleteAccountRequest(reason = "Moving away", password = "wrong_pw")

        assertThrows<IncorrectPasswordException> {
            service.requestAccountDeletion(userId, request)
        }
    }

    @Test
    fun `requestAccountDeletion soft-deletes user, deletes tokens, saves request, and notifies admins`() {
        val userId = UUID.randomUUID()
        val adminId = UUID.randomUUID()
        val userAuth = mockUserAuthDetails(userId = userId)

        every { userRepository.findAuthDetailsById(userId) } returns userAuth
        every { passwordEncoder.matches("correct_pw", any()) } returns true
        every { accountDeletionRequestRepository.deleteByUserId(userId) } returns 0
        every { userRepository.softDeleteById(userId) } returns 1
        every { accountDeletionRequestRepository.save(any()) } returnsArgument 0
        every { refreshTokenRepository.deleteAllByUserId(userId) } returns 2
        every { userRepository.findAllAdminIds() } returns listOf(adminId)

        val slot = slot<UserNotificationsEvent>()
        every { eventPublisher.publish(capture(slot)) } just Runs

        val request = DeleteAccountRequest(reason = "No longer needed", password = "correct_pw")
        service.requestAccountDeletion(userId, request)

        verify { userRepository.softDeleteById(userId) }
        verify { refreshTokenRepository.deleteAllByUserId(userId) }
        verify { accountDeletionRequestRepository.save(match { it.reason == "No longer needed" }) }
        assertThat(slot.captured.notifications).hasSize(1)
        assertThat(slot.captured.notifications[0].userId).isEqualTo(adminId)
    }

    @Test
    fun `reactivateAccount throws InvalidCredentialsException when password does not match`() {
        val deletedUser = mockDeletedUserCheck(userId = UUID.randomUUID())
        every { userRepository.findDeletedByNationalId("29001010101010") } returns deletedUser
        every { passwordEncoder.matches("wrong_pw", any()) } returns false

        val request = ReactivateAccountRequest(nationalId = "29001010101010", password = "wrong_pw")

        assertThrows<InvalidCredentialsException> {
            service.reactivateAccount(request)
        }
    }

    @Test
    fun `reactivateAccount restores user, deletes request, notifies admins, and returns auth response`() {
        val userId = UUID.randomUUID()
        val adminId = UUID.randomUUID()
        val deletedUser = mockDeletedUserCheck(userId = userId)
        val userRef = buildSampleUser(id = userId)

        every { userRepository.findDeletedByNationalId("29001010101010") } returns deletedUser
        every { passwordEncoder.matches("correct_pw", any()) } returns true
        every { userRepository.restoreUser(userId) } just Runs
        every { accountDeletionRequestRepository.deleteByUserId(userId) } returns 1
        every { jwtUtil.generateAccessToken(userId) } returns "access-token-123"
        every { jwtUtil.generateRefreshToken(userId) } returns "refresh-token-123"
        every { userRepository.getReferenceById(userId) } returns userRef
        every { refreshTokenRepository.save(any()) } returnsArgument 0
        every { userRepository.findAllAdminIds() } returns listOf(adminId)

        val slot = slot<UserNotificationsEvent>()
        every { eventPublisher.publish(capture(slot)) } just Runs

        val request = ReactivateAccountRequest(nationalId = "29001010101010", password = "correct_pw")
        val response = service.reactivateAccount(request)

        assertThat(response.accessToken).isEqualTo("access-token-123")
        assertThat(response.refreshToken).isEqualTo("refresh-token-123")
        verify { userRepository.restoreUser(userId) }
        verify { accountDeletionRequestRepository.deleteByUserId(userId) }
        assertThat(slot.captured.notifications).hasSize(1)
        assertThat(slot.captured.notifications[0].userId).isEqualTo(adminId)
    }

    @Test
    fun `getDeletionRequests throws UnauthorizedException for non-admin`() {
        val callerId = UUID.randomUUID()
        every { userRepository.findRoleById(callerId) } returns UserRole.KHADEM

        assertThrows<UnauthorizedException> {
            service.getDeletionRequests(callerId, PageRequest.of(0, 10))
        }
    }

    @Test
    fun `getDeletionRequests returns paged deletion requests for admin`() {
        val adminId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val requestId = UUID.randomUUID()
        val proj = mockDeletionRequestProjection(
            requestId = requestId,
            userId = userId,
            userName = "Mina George",
            userImageUrl = "photo.jpg?time=123"
        )

        every { userRepository.findRoleById(adminId) } returns UserRole.ADMIN
        every { accountDeletionRequestRepository.findAllPaged(any()) } returns PageImpl(listOf(proj))

        val result = service.getDeletionRequests(adminId, PageRequest.of(0, 10))

        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].reason).isEqualTo("Reason 1")
        assertThat(result.content[0].userName).isEqualTo("Mina George")
        assertThat(result.content[0].userImageUrl).isEqualTo("http://localhost:8080/profiles/photo.jpg?time=123")
    }

    @Test
    fun `approveDeletion removes request by id without modifying user`() {
        val adminId = UUID.randomUUID()
        val requestId = UUID.randomUUID()

        every { userRepository.findRoleById(adminId) } returns UserRole.ADMIN
        every { accountDeletionRequestRepository.deleteByRequestId(requestId) } returns 1

        service.approveDeletion(adminId, requestId)

        verify { accountDeletionRequestRepository.deleteByRequestId(requestId) }
        verify(exactly = 0) { userRepository.restoreUser(any()) }
    }

    @Test
    fun `rejectDeletion restores user and publishes admin notification`() {
        val adminId = UUID.randomUUID()
        val requestId = UUID.randomUUID()
        val targetUserId = UUID.randomUUID()
        val proj = mockDeletionRequestProjection(requestId = requestId, userId = targetUserId, userName = "Mina George")

        every { userRepository.findRoleById(adminId) } returns UserRole.ADMIN
        every { accountDeletionRequestRepository.findProjectionById(requestId) } returns proj
        every { userRepository.restoreUser(targetUserId) } just Runs
        every { accountDeletionRequestRepository.deleteByRequestId(requestId) } returns 1
        every { userRepository.findAllAdminIds() } returns listOf(adminId)

        val slot = slot<UserNotificationsEvent>()
        every { eventPublisher.publish(capture(slot)) } just Runs

        service.rejectDeletion(adminId, requestId)

        verify { userRepository.restoreUser(targetUserId) }
        verify { accountDeletionRequestRepository.deleteByRequestId(requestId) }
        assertThat(slot.captured.notifications).hasSize(1)
        assertThat(slot.captured.notifications[0].userId).isEqualTo(adminId)
    }
}
