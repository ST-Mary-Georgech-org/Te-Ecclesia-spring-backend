package org.teEcclesia.identity.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.teEcclesia.identity.entity.lookups.EducationalStage

interface EducationalStageRepository : JpaRepository<EducationalStage, Long>
