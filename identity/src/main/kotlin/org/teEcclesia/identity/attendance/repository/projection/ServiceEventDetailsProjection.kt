package org.teEcclesia.identity.attendance.repository.projection

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

interface ServiceEventDetailsProjection {
    fun getId(): Long
    fun getServiceId(): Long
    fun getName(): String?
    fun getEventDate(): LocalDate
    fun getStartTime(): LocalTime
    fun getEndTime(): LocalTime
    fun getAttendeeCount(): Long
    fun getRepeatedEventId(): Long?
    fun getCreatedAt(): Instant
}
