package org.teEcclesia.identity.attendance.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.teEcclesia.identity.attendance.entity.ChurchService
import org.teEcclesia.identity.attendance.repository.projection.ResponsibleServantProjection
import java.util.UUID

interface ChurchServiceRepository : JpaRepository<ChurchService, Long> {
    @Query(
        value = "SELECT cs FROM ChurchService cs ORDER BY cs.createdAt DESC",
        countQuery = "SELECT count(cs) FROM ChurchService cs"
    )
    fun findAllWithEducationalStages(pageable: Pageable): Page<ChurchService>

    @Query("SELECT cs FROM ChurchService cs LEFT JOIN FETCH cs.educationalStages WHERE cs.id = :id")
    fun findByIdWithEducationalStages(@Param("id") id: Long): ChurchService?

    @Query(
        value = """
            SELECT 
                rs.service_id AS "serviceId",
                u.id AS "id",
                u.first_name AS "firstName",
                u.second_name AS "secondName",
                u.third_name AS "thirdName",
                u.last_name AS "lastName",
                u.code AS "code",
                u.image_url AS "imageUrl"
            FROM identity.church_service_responsible_servants rs
            JOIN identity.users u ON u.id = rs.servant_id AND u.deleted = false
            WHERE rs.service_id IN :serviceIds
        """,
        nativeQuery = true
    )
    fun findResponsibleServantsByServiceIds(@Param("serviceIds") serviceIds: Collection<Long>): List<ResponsibleServantProjection>

    @Query(
        value = """
            SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END
            FROM identity.church_service_responsible_servants rs
            JOIN identity.users u ON u.id = rs.servant_id AND u.deleted = false
            WHERE rs.service_id = :serviceId AND rs.servant_id = :servantId
        """,
        nativeQuery = true
    )
    fun isServantResponsibleForService(@Param("serviceId") serviceId: Long, @Param("servantId") servantId: UUID): Boolean

    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<ChurchService>
    fun findAllByOrderByCreatedAtDesc(): List<ChurchService>
}
