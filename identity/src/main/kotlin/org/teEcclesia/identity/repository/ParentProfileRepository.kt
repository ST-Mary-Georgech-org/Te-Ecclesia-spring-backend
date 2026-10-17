package org.teEcclesia.identity.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.teEcclesia.identity.entity.ParentProfile
import java.util.UUID

interface ParentProfileRepository : JpaRepository<ParentProfile, Long> {
    @Query("SELECT COUNT(pp) > 0 FROM ParentProfile pp JOIN pp.children c WHERE pp.user.id = :parentId AND c.id = :childId")
    fun isChildOfParent(@Param("parentId") parentId: UUID, @Param("childId") childId: UUID): Boolean
}

