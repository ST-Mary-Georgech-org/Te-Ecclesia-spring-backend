package org.teEcclesia.notifications.eventListener


import org.slf4j.LoggerFactory
import org.teEcclesia.events.notifications.UserNotificationsEvent
import org.teEcclesia.notifications.service.NotificationService
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class NotificationListener(
    private val notificationService: NotificationService
) {
    private val log = LoggerFactory.getLogger(NotificationListener::class.java)

    @Async
    @EventListener
    fun handleUserNotifications(event: UserNotificationsEvent) {
        try {
            notificationService.saveNotifications(event.notifications)
            log.info("Successfully saved bulk notifications to database.")
        } catch (e: Exception) {
            log.error("Failed to save bulk notifications to database", e)
        }
    }
}