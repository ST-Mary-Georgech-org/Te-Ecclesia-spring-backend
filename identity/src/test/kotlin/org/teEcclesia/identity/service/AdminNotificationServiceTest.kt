package org.teEcclesia.identity.service

import com.google.common.truth.Truth.assertThat
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.teEcclesia.events.notifications.UserNotificationsEvent
import org.teEcclesia.events.publisher.TeEcclesiaEventPublisher
import org.teEcclesia.identity.api.dto.request.AdminSendNotificationRequest
import org.teEcclesia.identity.entity.enums.UserRole
import org.teEcclesia.identity.entity.enums.UserStatus
import org.teEcclesia.identity.exception.UnauthorizedException
import org.teEcclesia.identity.repository.UserRepository
import java.util.UUID

class AdminNotificationServiceTest {

    private val userRepository: UserRepository = mockk()
    private val eventPublisher: TeEcclesiaEventPublisher = mockk(relaxed = true)

    private lateinit var service: AdminNotificationService

    @BeforeEach
    fun setUp() {
        service = AdminNotificationService(userRepository, eventPublisher)
    }

    @Test
    fun `sendNotification throws UnauthorizedException when caller is not admin`() {
        val callerId = UUID.randomUUID()
        every { userRepository.findRoleById(callerId) } returns UserRole.KHADEM

        val request = AdminSendNotificationRequest(
            title = "Test",
            body = "Hello",
            userIds = listOf(UUID.randomUUID())
        )

        assertThrows<UnauthorizedException> {
            service.sendNotification(callerId, request)
        }
    }

    @Test
    fun `sendNotification publishes event for specific userIds`() {
        val callerId = UUID.randomUUID()
        val targetId1 = UUID.randomUUID()
        val targetId2 = UUID.randomUUID()

        every { userRepository.findRoleById(callerId) } returns UserRole.ADMIN

        val request = AdminSendNotificationRequest(
            title = "Title",
            body = "Body",
            userIds = listOf(targetId1, targetId2, targetId1),
            dataPayload = mapOf("key" to "value")
        )

        val slot = slot<UserNotificationsEvent>()
        every { eventPublisher.publish(capture(slot)) } just Runs

        val response = service.sendNotification(callerId, request)

        assertThat(response.recipientCount).isEqualTo(2)
        assertThat(slot.captured.notifications).hasSize(2)
        assertThat(slot.captured.notifications[0].subject).isEqualTo("Title")
        assertThat(slot.captured.notifications[0].message).isEqualTo("Body")
        assertThat(slot.captured.notifications[0].dataPayload["key"]).isEqualTo("value")
    }

    @Test
    fun `sendNotification queries target users when target group is specified`() {
        val callerId = UUID.randomUUID()
        val targetId1 = UUID.randomUUID()

        every { userRepository.findRoleById(callerId) } returns UserRole.ADMIN
        every {
            userRepository.findTargetUserIds(
                status = UserStatus.APPROVED,
                stageId = 10L,
                role = UserRole.MAKHDOOM
            )
        } returns listOf(targetId1)

        val request = AdminSendNotificationRequest(
            title = "Title",
            body = "Body",
            role = UserRole.MAKHDOOM,
            educationalStageId = 10L
        )

        val slot = slot<UserNotificationsEvent>()
        every { eventPublisher.publish(capture(slot)) } just Runs

        val response = service.sendNotification(callerId, request)

        assertThat(response.recipientCount).isEqualTo(1)
        assertThat(slot.captured.notifications[0].userId).isEqualTo(targetId1)
    }

    @Test
    fun `sendNotification resets stageId when role does not support educational stages`() {
        val callerId = UUID.randomUUID()
        val targetId1 = UUID.randomUUID()

        every { userRepository.findRoleById(callerId) } returns UserRole.ADMIN
        every {
            userRepository.findTargetUserIds(
                status = UserStatus.APPROVED,
                stageId = null,
                role = UserRole.KAHEN
            )
        } returns listOf(targetId1)

        val request = AdminSendNotificationRequest(
            title = "Title",
            body = "Body",
            role = UserRole.KAHEN,
            educationalStageId = 10L
        )

        val slot = slot<UserNotificationsEvent>()
        every { eventPublisher.publish(capture(slot)) } just Runs

        val response = service.sendNotification(callerId, request)

        assertThat(response.recipientCount).isEqualTo(1)
        assertThat(slot.captured.notifications[0].userId).isEqualTo(targetId1)
    }

    @Test
    fun `sendNotification throws IllegalArgumentException when no recipients found`() {
        val callerId = UUID.randomUUID()

        every { userRepository.findRoleById(callerId) } returns UserRole.ADMIN
        every {
            userRepository.findTargetUserIds(any(), any(), any())
        } returns emptyList()

        val request = AdminSendNotificationRequest(
            title = "Title",
            body = "Body",
            role = UserRole.MAKHDOOM
        )

        assertThrows<IllegalArgumentException> {
            service.sendNotification(callerId, request)
        }
    }
}
