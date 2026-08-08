package org.teEcclesia.identity.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import org.teEcclesia.identity.entity.lookups.EducationalStage
import org.teEcclesia.identity.entity.lookups.EducationalYear


import org.hibernate.envers.Audited
import org.hibernate.annotations.BatchSize

@Audited
@Entity
@BatchSize(size = 25)
@Table(name = "khadem_profiles", schema = "identity")
data class KhademProfile(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "educational_stage_id", nullable = false)
    val educationalStage: EducationalStage,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "educational_year_id", nullable = true)
    val educationalYear: EducationalYear? = null,

    @Column(nullable = false)
    val canApproveRequests: Boolean = false,

    @BatchSize(size = 25)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "khadem_responsible_stages",
        schema = "identity",
        joinColumns = [JoinColumn(name = "khadem_profile_id")],
        inverseJoinColumns = [JoinColumn(name = "educational_stage_id")]
    )
    val responsibleStages: List<EducationalStage> = emptyList(),

    @BatchSize(size = 25)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "khadem_responsible_years",
        schema = "identity",
        joinColumns = [JoinColumn(name = "khadem_profile_id")],
        inverseJoinColumns = [JoinColumn(name = "educational_year_id")]
    )
    val responsibleYears: List<EducationalYear> = emptyList()
)
