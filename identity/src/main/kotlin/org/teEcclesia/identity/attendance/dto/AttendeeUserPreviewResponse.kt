package org.teEcclesia.identity.attendance.dto

import org.teEcclesia.identity.entity.enums.UserRole

data class AttendeeUserPreviewResponse(
    val id: String,
    val name: String,
    val role: UserRole,
    val stageName: String?,
    val yearName: String?,
    val code: String?,
    val imageUrl: String? = null
)
