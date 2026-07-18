package org.teEcclesia.identity.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.teEcclesia.identity.entity.lookups.EducationalStage

interface EducationalStageRepository : JpaRepository<EducationalStage, Long> {
    fun findByIdIn(ids: Set<Long>): List<EducationalStage>
    fun findByYearsIdIn(ids: Set<Long>): List<EducationalStage>
}
