package org.teEcclesia.identity.eventListener

import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.data.domain.PageRequest
import org.teEcclesia.events.identity.UserPendingApprovalEvent
import org.teEcclesia.events.notifications.PushNotificationEvent
import org.teEcclesia.events.notifications.UserNotificationsEvent
import org.teEcclesia.events.notifications.utils.NotificationMedium
import org.teEcclesia.events.notifications.utils.NotificationType
import org.teEcclesia.events.notifications.NotificationDetails
import org.teEcclesia.events.publisher.TeEcclesiaEventPublisher
import org.teEcclesia.identity.entity.enums.UserRole
import org.teEcclesia.identity.repository.UserRepository

@Component
class UserVerificationListener(
    private val userRepository: UserRepository,
    private val publisher: TeEcclesiaEventPublisher
) {
    private val log = LoggerFactory.getLogger(UserVerificationListener::class.java)

    @Async
    @EventListener
    fun handleUserPendingApproval(event: UserPendingApprovalEvent) {
        log.info("Handling UserPendingApprovalEvent for user: ${event.userId}")
        val batchSize = 500
        var page = 0
        var hasMore = true

        while (hasMore) {
            val pageable = PageRequest.of(page, batchSize)
            val khademsPage = userRepository.findByRole(UserRole.KHADEM, pageable) //TODO: and can approve

            if (khademsPage.isEmpty) {
                hasMore = false
                break
            }

            val notifications = khademsPage.content.map { khadem ->
                NotificationDetails(
                    userId = khadem.id,
                    subject = "New User Verification",
                    message = "User ${event.userName} has verified their phone and is awaiting your review.",
                    type = NotificationType.SYSTEM,
                    medium = NotificationMedium.PUSH
                )
            }

            publisher.publish(UserNotificationsEvent(notifications))
            
            page++
            hasMore = khademsPage.hasNext()
        }
    }
}
