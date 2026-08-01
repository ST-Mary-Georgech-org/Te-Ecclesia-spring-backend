package org.teEcclesia.identity.attendance.dto

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

data class ServiceEventResponse(
    val id: Long,
    val serviceId: Long,
    val name: String?,
    val eventDate: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val createdAt: Instant
)
