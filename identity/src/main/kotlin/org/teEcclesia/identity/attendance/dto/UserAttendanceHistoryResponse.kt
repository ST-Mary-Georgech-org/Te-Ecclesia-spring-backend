package org.teEcclesia.identity.attendance.dto

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

data class UserAttendanceHistoryResponse(
    val id: Long,
    val eventId: Long,
    val serviceId: Long,
    val serviceName: String,
    val eventName: String?,
    val eventDate: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val registeredAt: Instant
)
