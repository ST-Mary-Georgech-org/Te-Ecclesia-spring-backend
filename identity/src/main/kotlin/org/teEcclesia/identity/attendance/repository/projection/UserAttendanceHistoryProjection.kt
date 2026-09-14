package org.teEcclesia.identity.attendance.repository.projection

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

interface UserAttendanceHistoryProjection {
    fun getId(): Long
    fun getEventId(): Long
    fun getServiceId(): Long
    fun getServiceName(): String
    fun getEventName(): String?
    fun getEventDate(): LocalDate
    fun getStartTime(): LocalTime
    fun getEndTime(): LocalTime
    fun getRegisteredAt(): Instant
}
