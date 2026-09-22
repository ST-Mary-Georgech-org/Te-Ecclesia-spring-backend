package org.teEcclesia.identity.attendance.repository

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.teEcclesia.identity.attendance.entity.RepeatedEvent
import java.time.LocalDate

interface RepeatedEventRepository : JpaRepository<RepeatedEvent, Long> {
    fun findByServiceId(serviceIds: Long): RepeatedEvent?

    fun findByNextCreationDate(
        date: LocalDate
    ): List<RepeatedEvent>

    @Query("""
        SELECT r FROM RepeatedEvent r 
        WHERE r.nextCreationDate <= :today AND r.id > :lastId 
        ORDER BY r.id ASC
    """)
    fun findDueRepeatedEvents(
        @Param("today") today: LocalDate,
        @Param("lastId") lastId: Long,
        pageable: Pageable
    ): List<RepeatedEvent>
}