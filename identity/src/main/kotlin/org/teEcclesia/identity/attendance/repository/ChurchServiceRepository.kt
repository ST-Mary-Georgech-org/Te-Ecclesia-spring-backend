package org.teEcclesia.identity.attendance.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.teEcclesia.identity.attendance.entity.ChurchService

interface ChurchServiceRepository : JpaRepository<ChurchService, Long> {
    fun findAllByOrderByCreatedAtDesc(): List<ChurchService>
}
