package org.teEcclesia.notifications.integration

import com.google.common.truth.Truth
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.teEcclesia.events.notifications.NotificationDetails
import org.teEcclesia.events.notifications.utils.NotificationType as EventNotificationType
import org.teEcclesia.notifications.NotificationsTestApplication
import org.teEcclesia.notifications.entity.NotificationType
import org.teEcclesia.notifications.repository.NotificationRepository
import org.teEcclesia.notifications.service.NotificationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.util.UUID

@SpringBootTest(classes = [NotificationsTestApplication::class])
@ActiveProfiles("test")
class NotificationServiceIntegrationTest {

    @Autowired
    private lateinit var notificationService: NotificationService

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @BeforeEach
    fun setUp() {
        notificationRepository.deleteAll()
    }

    @Test
    fun `should save notification directly to database`() {

        val userId = UUID.randomUUID()
        val title = "Achievement Unlocked!"
        val message = "You got a new badge."
        val type = EventNotificationType.SYSTEM

        val notificationDetails = NotificationDetails(
            userId = userId,
            subject = title,
            message = message,
            type = type
        )
        notificationService.saveNotification(notificationDetails)


        val allNotifications = notificationRepository.findAll()

        Truth.assertThat(allNotifications).hasSize(1)
        Truth.assertThat(allNotifications[0].userId).isEqualTo(userId)
        Truth.assertThat(allNotifications[0].title).isEqualTo(title)
        Truth.assertThat(allNotifications[0].message).isEqualTo(message)
        Truth.assertThat(allNotifications[0].isRead).isFalse()
    }

    @Test
    fun `should count unread notifications correctly`() {

        val userId = UUID.randomUUID()

        notificationService.saveNotification(NotificationDetails(userId, "T1", "M1", EventNotificationType.SYSTEM))
        notificationService.saveNotification(NotificationDetails(userId, "T2", "M2", EventNotificationType.SYSTEM))
        notificationService.saveNotification(NotificationDetails(userId, "T3", "M3", EventNotificationType.SYSTEM))

        val unreadCount = notificationService.getUnreadCount(userId)

        Truth.assertThat(unreadCount).isEqualTo(3)
    }

    @Test
    fun `should save multiple notifications to database`() {
        val userId = UUID.randomUUID()
        val notifications = listOf(
            NotificationDetails(userId, "Title 1", "Msg 1", EventNotificationType.SYSTEM),
            NotificationDetails(userId, "Title 2", "Msg 2", EventNotificationType.SYSTEM)
        )

        notificationService.saveNotifications(notifications)

        val allNotifications = notificationRepository.findAll()
        Truth.assertThat(allNotifications).hasSize(2)

        val notification1 = allNotifications.first { it.title == "Title 1" }
        Truth.assertThat(notification1.userId).isEqualTo(userId)
        Truth.assertThat(notification1.message).isEqualTo("Msg 1")
        Truth.assertThat(notification1.type).isEqualTo(NotificationType.SYSTEM)

        val notification2 = allNotifications.first { it.title == "Title 2" }
        Truth.assertThat(notification2.userId).isEqualTo(userId)
        Truth.assertThat(notification2.message).isEqualTo("Msg 2")
        Truth.assertThat(notification2.type).isEqualTo(NotificationType.SYSTEM)
    }
}
