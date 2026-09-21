package org.teEcclesia.identity.attendance.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.teEcclesia.identity.attendance.entity.RepeatedEvent
import java.time.LocalDate

interface RepeatedEventRepository : JpaRepository<RepeatedEvent, Long> {
    fun findByServiceIdIn(serviceIds: List<Long>): List<RepeatedEvent>

    fun findByServiceId(serviceIds: Long): RepeatedEvent?

    fun findByNextCreationDate(
        date: LocalDate
    ): List<RepeatedEvent>

}