package org.teEcclesia.notifications.service

import com.google.firebase.messaging.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.teEcclesia.events.identity.DeviceTokenUnregisteredEvent
import org.teEcclesia.events.publisher.TeEcclesiaEventPublisher
import org.springframework.stereotype.Service

@Service
class PushNotificationService(
    private val eventPublisher: TeEcclesiaEventPublisher
) {

    val logger: Logger = LoggerFactory.getLogger(PushNotificationService::class.java)

    fun sendPushNotificationToTokens(
        tokens: List<String>,
        title: String,
        body: String,
        dataPayload: Map<String, String> = emptyMap()
    ) {
        tokens.forEach { token ->
            sendPushNotificationToToken(token, title, body, dataPayload)
        }
    }

    private fun sendPushNotificationToToken(
        token: String,
        title: String,
        body: String,
        dataPayload: Map<String, String>
    ) {
        try {
            val messageBuilder = Message.builder()
                .setToken(token)
                .setNotification(
                    Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build()
                )
                .setAndroidConfig(
                    AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .build()
                )
                .setApnsConfig(
                    ApnsConfig.builder()
                        .setAps(Aps.builder().setContentAvailable(true).build())
                        .putHeader("apns-priority", "10")
                        .build()
                )

            if (dataPayload.isNotEmpty()) {
                messageBuilder.putAllData(dataPayload + mapOf("title" to title, "body" to body))
            }

            val message = messageBuilder.build()
            try {
                val response = FirebaseMessaging.getInstance().send(message)
                logger.info("Successfully sent message to token $token: $response")
            } catch (e: FirebaseMessagingException) {
                when (e.messagingErrorCode) {
                    MessagingErrorCode.UNREGISTERED,
                    MessagingErrorCode.INVALID_ARGUMENT -> {
                        logger.warn("Push token is unregistered or invalid: $token. Publishing clean-up event.")
                        eventPublisher.publish(DeviceTokenUnregisteredEvent(token))
                    }
                    else -> {
                        logger.error("Failed to send push notification due to FCM error: ${e.message}", e)
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to send push notification to token $token: ${e.message}", e)
        }
    }
}
