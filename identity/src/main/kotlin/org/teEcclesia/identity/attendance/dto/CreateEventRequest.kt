package org.teEcclesia.identity.attendance.dto

import java.time.LocalDate
import java.time.LocalTime

data class CreateEventRequest(
    val name: String? = null,
    val eventDate: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime
)
