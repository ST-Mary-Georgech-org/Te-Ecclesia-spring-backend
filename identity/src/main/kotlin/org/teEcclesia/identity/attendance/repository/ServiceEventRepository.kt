package org.teEcclesia.identity.attendance.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.teEcclesia.identity.attendance.entity.ServiceEvent
import org.teEcclesia.identity.attendance.repository.projection.ServiceEventDetailsProjection
import java.time.LocalDate

interface ServiceEventRepository : JpaRepository<ServiceEvent, Long> {
    @Query(
        value = """
            SELECT 
                se.id AS id,
                se.serviceId AS serviceId,
                se.name AS name,
                se.eventDate AS eventDate,
                se.startTime AS startTime,
                se.endTime AS endTime,
                se.createdAt AS createdAt,
                se.repeatedEventId AS repeatedEventId,
                COUNT(ea.id) AS attendeeCount
            FROM ServiceEvent se
            LEFT JOIN EventAttendee ea ON ea.eventId = se.id
            WHERE se.serviceId = :serviceId
            GROUP BY se.id, se.serviceId, se.name, se.eventDate, se.startTime, se.endTime, se.createdAt , se.repeatedEventId 
            ORDER BY se.eventDate DESC, se.startTime DESC
        """,
        countQuery = "SELECT COUNT(se) FROM ServiceEvent se WHERE se.serviceId = :serviceId"
    )
    fun findEventsWithAttendeeCountByServiceId(
        @Param("serviceId") serviceId: Long,
        pageable: Pageable
    ): Page<ServiceEventDetailsProjection>

    fun findByRepeatedEventIdAndEventDate(
        repeatedEventId: Long,
        eventDate: LocalDate
    ): ServiceEvent?

    fun findAllByServiceIdOrderByEventDateDescStartTimeDesc(serviceId: Long, pageable: Pageable): Page<ServiceEvent>
    fun findAllByServiceIdOrderByEventDateDescStartTimeDesc(serviceId: Long): List<ServiceEvent>
}
