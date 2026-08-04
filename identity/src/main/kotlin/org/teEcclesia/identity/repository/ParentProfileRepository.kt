package org.teEcclesia.identity.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.teEcclesia.identity.entity.ParentProfile

interface ParentProfileRepository : JpaRepository<ParentProfile, Long>
