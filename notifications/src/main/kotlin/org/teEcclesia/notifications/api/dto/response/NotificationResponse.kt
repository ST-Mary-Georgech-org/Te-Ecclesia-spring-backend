package org.teEcclesia.notifications.api.dto.response

import org.teEcclesia.notifications.entity.NotificationType
import java.time.Instant
import java.util.UUID

data class NotificationResponse(
    val id: UUID,
    val title: String,
    val message: String,
    val type: NotificationType,
    val sentAt: Instant,
    val isRead: Boolean
)