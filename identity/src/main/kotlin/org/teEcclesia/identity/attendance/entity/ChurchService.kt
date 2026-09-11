package org.teEcclesia.identity.attendance.entity

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.teEcclesia.identity.entity.lookups.EducationalStage
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "church_services")
data class ChurchService(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val name: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "educational_stage_id", nullable = true)
    val educationalStage: EducationalStage? = null,

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "church_service_responsible_servants",
        joinColumns = [JoinColumn(name = "service_id")]
    )
    @Column(name = "servant_id")
    val responsibleServantIds: MutableSet<UUID> = mutableSetOf(),

    @Column(name = "created_by_id", nullable = false)
    val createdById: UUID,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
