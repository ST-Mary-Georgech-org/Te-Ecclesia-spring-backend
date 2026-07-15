package org.teEcclesia.identity.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.teEcclesia.identity.entity.ParentProfile
import java.util.UUID

@Repository
interface ParentProfileRepository : JpaRepository<ParentProfile, Long>
