package org.teEcclesia.identity.attendance.dto

import org.teEcclesia.identity.entity.enums.UserRole
import java.time.Instant

data class EventAttendeeResponse(
    val id: Long,
    val eventId: Long,
    val userId: String,
    val name: String,
    val role: UserRole,
    val stageName: String?,
    val yearName: String?,
    val registeredAt: Instant
)
