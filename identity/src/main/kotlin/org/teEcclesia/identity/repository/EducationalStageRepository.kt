package org.teEcclesia.identity.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.teEcclesia.identity.entity.lookups.EducationalStage

interface EducationalStageRepository : JpaRepository<EducationalStage, Long> {
    @EntityGraph(attributePaths = ["years"])
    override fun findAll(pageable: Pageable): Page<EducationalStage>

    @EntityGraph(attributePaths = ["years"])
    fun findByIdIn(ids: Set<Long>): List<EducationalStage>

    @EntityGraph(attributePaths = ["years"])
    fun findByYearsIdIn(ids: Set<Long>): List<EducationalStage>
}
