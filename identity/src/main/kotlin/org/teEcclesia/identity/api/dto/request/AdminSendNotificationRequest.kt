package org.teEcclesia.identity.api.dto.request

import jakarta.validation.constraints.NotBlank
import org.teEcclesia.identity.entity.enums.UserRole
import java.util.UUID

data class AdminSendNotificationRequest(
    @field:NotBlank(message = "{validation.title.required}")
    val title: String,

    @field:NotBlank(message = "{validation.body.required}")
    val body: String,

    val userIds: List<UUID>? = null,
    val role: UserRole? = null,
    val educationalStageId: Long? = null,
    val dataPayload: Map<String, String>? = null
)
