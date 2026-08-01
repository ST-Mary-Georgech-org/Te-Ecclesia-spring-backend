package org.teEcclesia.identity.attendance.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.teEcclesia.identity.attendance.entity.ServiceEvent

@Repository
interface ServiceEventRepository : JpaRepository<ServiceEvent, Long> {
    fun findAllByServiceIdOrderByEventDateDescStartTimeDesc(serviceId: Long): List<ServiceEvent>
}
