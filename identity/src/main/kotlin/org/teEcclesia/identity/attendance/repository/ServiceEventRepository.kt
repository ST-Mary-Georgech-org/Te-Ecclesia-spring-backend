package org.teEcclesia.identity.attendance.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.teEcclesia.identity.attendance.entity.ServiceEvent

interface ServiceEventRepository : JpaRepository<ServiceEvent, Long> {
    fun findAllByServiceIdOrderByEventDateDescStartTimeDesc(serviceId: Long, pageable: Pageable): Page<ServiceEvent>
    fun findAllByServiceIdOrderByEventDateDescStartTimeDesc(serviceId: Long): List<ServiceEvent>
}
