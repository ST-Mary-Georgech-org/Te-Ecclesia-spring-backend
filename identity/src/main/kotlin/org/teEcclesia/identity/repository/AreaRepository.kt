package org.teEcclesia.identity.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.teEcclesia.identity.entity.lookups.Area

interface AreaRepository : JpaRepository<Area, Long> {
    fun findByName(name: String): Area?

    @Query("""
        SELECT a FROM Area a 
        WHERE :query IS NULL OR TRIM(:query) = '' 
        OR LOWER(a.name) LIKE LOWER(CONCAT('%', :query, '%'))
        ORDER BY 
          CASE WHEN LOWER(a.name) = LOWER(:query) THEN 0
               WHEN LOWER(a.name) LIKE LOWER(CONCAT(:query, '%')) THEN 1
               ELSE 2
          END,
          a.suggestedCount DESC,
          a.name ASC
    """)
    fun searchAreas(@Param("query") query: String?, pageable: Pageable): Page<Area>
}
