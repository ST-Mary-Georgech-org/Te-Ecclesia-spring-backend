package org.teEcclesia.notifications.eventListener

import org.slf4j.LoggerFactory
import org.teEcclesia.events.notifications.PushNotificationEvent
import org.teEcclesia.notifications.service.PushNotificationService
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class AsyncPushNotificationListener(
    private val pushNotificationService: PushNotificationService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @EventListener
    fun handlePushNotificationEvent(event: PushNotificationEvent) {
        try {
            pushNotificationService.sendPushNotificationToTokens(
                tokens = event.tokens,
                title = event.title,
                body = event.body,
                dataPayload = event.dataPayload + ("type" to event.type.name)
            )
            log.info("Push notifications sent successfully to ${event.tokens.size} tokens.")
        } catch (e: Exception) {
            log.error("Failed to send push notifications", e)
        }
    }
}
