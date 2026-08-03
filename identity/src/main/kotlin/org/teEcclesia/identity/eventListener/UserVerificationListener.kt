package org.teEcclesia.identity.eventListener

import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.data.domain.PageRequest
import org.teEcclesia.events.identity.UserPendingApprovalEvent
import org.teEcclesia.events.identity.UserApprovalRequestUpdatedEvent
import org.teEcclesia.events.notifications.UserNotificationsEvent
import org.teEcclesia.events.notifications.utils.NotificationMedium
import org.teEcclesia.events.notifications.utils.NotificationType
import org.teEcclesia.events.notifications.NotificationDetails
import org.teEcclesia.events.publisher.TeEcclesiaEventPublisher
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
            val approversPage = userRepository.findApprovers(pageable = pageable)

            if (approversPage.isEmpty) {
                hasMore = false
                break
            }

            val notifications = approversPage.content.map { approver ->
                NotificationDetails(
                    userId = approver.id,
                    subject = "New User Verification",
                    message = "User ${event.userName} has verified their phone and is awaiting your review.",
                    type = NotificationType.REVIEW,
                    medium = NotificationMedium.PUSH,
                    dataPayload = mapOf("id" to event.userId.toString())
                )
            }

            publisher.publish(UserNotificationsEvent(notifications))
            
            page++
            hasMore = approversPage.hasNext()
        }
    }

    @Async
    @EventListener
    fun handleUserApprovalRequestUpdated(event: UserApprovalRequestUpdatedEvent) {
        log.info("Handling UserApprovalRequestUpdatedEvent for user: ${event.userId}")
        val batchSize = 500
        var page = 0
        var hasMore = true

        while (hasMore) {
            val pageable = PageRequest.of(page, batchSize)
            val approversPage = userRepository.findApprovers(pageable = pageable)

            if (approversPage.isEmpty) {
                hasMore = false
                break
            }

            val notifications = approversPage.content.map { approver ->
                NotificationDetails(
                    userId = approver.id,
                    subject = "Update to User Verification Request",
                    message = "User ${event.userName} has updated their registration request details and is awaiting your review.",
                    type = NotificationType.REVIEW,
                    medium = NotificationMedium.PUSH,
                    dataPayload = mapOf("id" to event.userId.toString())
                )
            }

            publisher.publish(UserNotificationsEvent(notifications))
            
            page++
            hasMore = approversPage.hasNext()
        }
    }
}
