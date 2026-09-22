package org.teEcclesia.identity.attendance.dto

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

data class RepeatedEventResponse(
    val id: Long,
    val serviceId: Long,
    val name: String?,
    val startDate: LocalDate,
    val nextCreationDate: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val repeatEvery : Int,
    val createdAt: Instant
)
