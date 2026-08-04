package org.teEcclesia.identity.attendance.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.teEcclesia.identity.attendance.entity.EventAttendee
import java.util.UUID

interface EventAttendeeRepository : JpaRepository<EventAttendee, Long> {
    fun findAllByEventIdOrderByRegisteredAtDesc(eventId: Long): List<EventAttendee>
    fun existsByEventIdAndUserId(eventId: Long, userId: UUID): Boolean
    fun deleteByEventIdAndUserId(eventId: Long, userId: UUID)
}
