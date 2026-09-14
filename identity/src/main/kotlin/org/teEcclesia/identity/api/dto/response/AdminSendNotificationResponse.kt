package org.teEcclesia.identity.api.dto.response

data class AdminSendNotificationResponse(
    val recipientCount: Int,
    val message: String
)
