package org.teEcclesia.events.notifications

import org.teEcclesia.events.TeEcclesiaEvent
import org.teEcclesia.events.notifications.utils.NotificationMedium
import org.teEcclesia.events.notifications.utils.NotificationType
import java.util.*

data class NotificationDetails(
    val userId: UUID,
    val subject: String,
    val message: String,
    val type: NotificationType,
    val medium: NotificationMedium = NotificationMedium.EMAIL,
    val dataPayload: Map<String, String> = emptyMap()
)

data class UserNotificationsEvent(
    val notifications: List<NotificationDetails>
) : TeEcclesiaEvent