package org.teEcclesia.identity.attendance.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.teEcclesia.identity.attendance.entity.EventAttendee
import org.teEcclesia.identity.attendance.repository.projection.EventAttendeeProjection
import java.util.UUID

interface EventAttendeeRepository : JpaRepository<EventAttendee, Long> {
    @Query("""
        SELECT 
            ea.id as id,
            ea.eventId as eventId,
            ea.registeredAt as registeredAt,
            u.id as userId,
            u.firstName as firstName,
            u.secondName as secondName,
            u.thirdName as thirdName,
            u.lastName as lastName,
            u.role as role,
            ms.nameAr as makhdoomStageAr,
            ms.nameEn as makhdoomStageEn,
            my.nameAr as makhdoomYearAr,
            my.nameEn as makhdoomYearEn,
            ks.nameAr as khademStageAr,
            ks.nameEn as khademStageEn,
            ky.nameAr as khademYearAr,
            ky.nameEn as khademYearEn
        FROM EventAttendee ea
        JOIN ea.user u
        LEFT JOIN u.makhdoomProfile mp
        LEFT JOIN mp.educationalStage ms
        LEFT JOIN mp.educationalYear my
        LEFT JOIN u.khademProfile kp
        LEFT JOIN kp.educationalStage ks
        LEFT JOIN kp.educationalYear ky
        WHERE ea.eventId = :eventId
        ORDER BY ea.registeredAt DESC
    """)
    fun findAttendeesByEventId(
        @Param("eventId") eventId: Long,
        pageable: Pageable
    ): Page<EventAttendeeProjection>

    fun findAllByEventIdOrderByRegisteredAtDesc(eventId: Long, pageable: Pageable): Page<EventAttendee>

    @EntityGraph(
        attributePaths = [
            "user",
            "user.makhdoomProfile",
            "user.makhdoomProfile.educationalStage",
            "user.makhdoomProfile.educationalYear",
            "user.khademProfile",
            "user.khademProfile.educationalStage",
            "user.khademProfile.educationalYear"
        ]
    )
    fun findByEventIdAndUserId(eventId: Long, userId: UUID): EventAttendee?

    fun deleteByEventIdAndUserId(eventId: Long, userId: UUID)
}
