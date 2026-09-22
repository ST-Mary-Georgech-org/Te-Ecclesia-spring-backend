package org.teEcclesia.identity.attendance.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.BatchSize
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.SQLRestriction
import org.hibernate.envers.Audited
import org.hibernate.envers.NotAudited
import org.teEcclesia.identity.entity.lookups.EducationalStage
import java.time.Instant
import java.util.UUID

@Audited
@Entity
@Table(name = "church_services", schema = "identity")
@SQLDelete(sql = "UPDATE identity.church_services SET deleted = true WHERE id=?")
@SQLRestriction("deleted = false")
data class ChurchService(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val name: String,

    @NotAudited
    @OneToOne(
        mappedBy = "churchService",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    val repeatedEvent: RepeatedEvent? = null,

    @BatchSize(size = 25)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "church_service_educational_stages",
        schema = "identity",
        joinColumns = [JoinColumn(name = "service_id")],
        inverseJoinColumns = [JoinColumn(name = "educational_stage_id")]
    )
    val educationalStages: MutableSet<EducationalStage> = mutableSetOf(),

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "church_service_responsible_servants",
        schema = "identity",
        joinColumns = [JoinColumn(name = "service_id")]
    )
    @Column(name = "servant_id")
    val responsibleServantIds: MutableSet<UUID> = mutableSetOf(),

    @Column(name = "created_by_id", nullable = false)
    val createdById: UUID,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(nullable = false, columnDefinition = "boolean default false")
    val deleted: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChurchService) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "ChurchService(id=$id, name='$name')"
    }
}
