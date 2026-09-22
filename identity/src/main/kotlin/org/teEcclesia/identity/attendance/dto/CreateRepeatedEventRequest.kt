package org.teEcclesia.identity.attendance.dto

import java.time.LocalDate
import java.time.LocalTime

data class CreateRepeatedEventRequest(
    val name: String? = null,
    val startDate: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val repeatEvery : Int,
)
