package org.teEcclesia.identity.eventListener

import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.teEcclesia.events.identity.DeviceTokenUnregisteredEvent
import org.teEcclesia.events.notifications.EmailEvent
import org.teEcclesia.events.notifications.PushNotificationEvent
import org.teEcclesia.events.notifications.UserNotificationsEvent
import org.teEcclesia.events.notifications.utils.NotificationMedium
import org.teEcclesia.events.publisher.TeEcclesiaEventPublisher
import org.teEcclesia.identity.repository.RefreshTokenRepository
import org.teEcclesia.identity.service.UserService
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class UserNotificationListener(
    private val userService: UserService,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val publisher: TeEcclesiaEventPublisher
) {
    private val log = LoggerFactory.getLogger(UserNotificationListener::class.java)

    @Async
    @EventListener
    fun handleUserNotifications(event: UserNotificationsEvent) {
        try {

            val userIds = event.notifications.map { it.userId }.distinct()
            val emailsMap = userService.findEmailsByUserIds(userIds)

            event.notifications.forEach { notification ->

                val sendEmail = notification.medium == NotificationMedium.EMAIL || notification.medium == NotificationMedium.BOTH
                val sendPush = notification.medium == NotificationMedium.PUSH || notification.medium == NotificationMedium.BOTH

                if (sendEmail) {
                    val userEmail = emailsMap[notification.userId.toString()]
                    if(userEmail != null) {
                        publisher.publish(
                            EmailEvent(
                                to = userEmail,
                                subject = notification.subject,
                                text = notification.message
                            )
                        )
                    } else {
                        log.warn("Email not found for user ID: ${notification.userId}")
                    }
                }
                
                if (sendPush) {
                    val refreshTokens = refreshTokenRepository.findAllByUserId(notification.userId)
                    val deviceTokens = refreshTokens.mapNotNull { it.deviceToken }.distinct()

                    if (deviceTokens.isNotEmpty()) {
                        publisher.publish(
                            PushNotificationEvent(
                                userId = notification.userId,
                                tokens = deviceTokens,
                                title = notification.subject,
                                body = notification.message,
                                type = notification.type,
                                dataPayload = notification.dataPayload
                            )
                        )
                    } else {
                        log.warn("Device tokens not found for user ID: ${notification.userId}")
                    }
                }

            }
        } catch (e: Exception) {
            log.error("Error processing bulk notifications in Identity module", e)
        }
    }

    @Async
    @EventListener
    @Transactional
    fun handleDeviceTokenUnregistered(event: DeviceTokenUnregisteredEvent) {
        try {
            val updatedCount = refreshTokenRepository.clearDeviceToken(event.token)
            if (updatedCount > 0) {
                log.info("Successfully cleared unregistered device token: ${event.token} from $updatedCount sessions")
            }
        } catch (e: Exception) {
            log.error("Error clearing unregistered device token: ${event.token}", e)
        }
    }
}