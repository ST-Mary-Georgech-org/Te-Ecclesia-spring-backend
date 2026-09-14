package org.teEcclesia.identity.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.teEcclesia.events.notifications.NotificationDetails
import org.teEcclesia.events.notifications.UserNotificationsEvent
import org.teEcclesia.events.notifications.utils.NotificationMedium
import org.teEcclesia.events.notifications.utils.NotificationType
import org.teEcclesia.events.publisher.TeEcclesiaEventPublisher
import org.teEcclesia.identity.api.dto.request.AdminSendNotificationRequest
import org.teEcclesia.identity.api.dto.response.AdminSendNotificationResponse
import org.teEcclesia.identity.entity.enums.UserRole
import org.teEcclesia.identity.entity.enums.UserStatus
import org.teEcclesia.identity.exception.UnauthorizedException
import org.teEcclesia.identity.repository.UserRepository
import java.util.UUID

@Service
class AdminNotificationService(
    private val userRepository: UserRepository,
    private val eventPublisher: TeEcclesiaEventPublisher
) {
    @Transactional(readOnly = true)
    fun sendNotification(
        callerId: UUID,
        request: AdminSendNotificationRequest
    ): AdminSendNotificationResponse {
        val callerRole = userRepository.findRoleById(callerId)
        if (callerRole != UserRole.ADMIN) {
            throw UnauthorizedException("Only admins can send broadcast notifications")
        }

        val effectiveStageId = if (request.role != null && request.role != UserRole.KHADEM && request.role != UserRole.MAKHDOOM) {
            null
        } else {
            request.educationalStageId
        }

        val targetUserIds: List<UUID> = if (!request.userIds.isNullOrEmpty()) {
            request.userIds.distinct()
        } else if (effectiveStageId != null || request.role != null) {
            userRepository.findTargetUserIds(
                status = UserStatus.APPROVED,
                stageId = effectiveStageId,
                role = request.role
            )
        } else {
            userRepository.findTargetUserIds(
                status = UserStatus.APPROVED,
                stageId = null,
                role = null
            )
        }

        if (targetUserIds.isEmpty()) {
            throw IllegalArgumentException("No recipients found matching the specified criteria")
        }

        val resolvedType = request.dataPayload?.get("type")
            ?.let { typeStr -> runCatching { NotificationType.valueOf(typeStr.uppercase()) }.getOrNull() }
            ?: NotificationType.ALERT

        val notifications = targetUserIds.map { userId ->
            NotificationDetails(
                userId = userId,
                subject = request.title,
                message = request.body,
                type = resolvedType,
                medium = NotificationMedium.PUSH,
                dataPayload = request.dataPayload ?: emptyMap()
            )
        }

        eventPublisher.publish(UserNotificationsEvent(notifications))

        return AdminSendNotificationResponse(
            recipientCount = targetUserIds.size,
            message = "Notification sent successfully to ${targetUserIds.size} recipients"
        )
    }
}
