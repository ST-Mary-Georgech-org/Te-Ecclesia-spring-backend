package org.teEcclesia.identity.attendance.dto

import org.teEcclesia.identity.attendance.repository.projection.UserAttendanceHistoryProjection
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

fun UserAttendanceHistoryProjection.toUserAttendanceHistoryResponse() = UserAttendanceHistoryResponse(
    id = getId(),
    eventId = getEventId(),
    serviceId = getServiceId(),
    serviceName = getServiceName(),
    eventName = getEventName(),
    eventDate = getEventDate(),
    startTime = getStartTime(),
    endTime = getEndTime(),
    registeredAt = getRegisteredAt()
)