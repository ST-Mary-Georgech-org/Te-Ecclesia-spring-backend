package org.teEcclesia.identity.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.teEcclesia.identity.entity.lookups.Area

interface AreaRepository : JpaRepository<Area, Long> {
    fun findByName(name: String): Area?
}
