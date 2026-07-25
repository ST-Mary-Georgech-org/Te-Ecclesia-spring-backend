package org.teEcclesia.identity.entity.lookups

import jakarta.persistence.*

import org.hibernate.envers.Audited

@Audited
@Entity
@Table(name = "educational_years", schema = "identity")
data class EducationalYear(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val nameAr: String,

    @Column(nullable = false)
    val nameEn: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", nullable = false)
    val stage: EducationalStage
)
