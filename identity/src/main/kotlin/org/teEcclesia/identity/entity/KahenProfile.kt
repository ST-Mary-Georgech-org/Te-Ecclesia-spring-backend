package org.teEcclesia.identity.entity

import jakarta.persistence.*
import org.teEcclesia.identity.entity.lookups.EducationalStage
import org.hibernate.envers.Audited
import java.time.LocalDate

@Audited
@Entity
@Table(name = "kahen_profiles", schema = "identity")
data class KahenProfile(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    val user: User,

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "kahen_educational_stages",
        schema = "identity",
        joinColumns = [JoinColumn(name = "kahen_profile_id")],
        inverseJoinColumns = [JoinColumn(name = "educational_stage_id")]
    )
    val educationalStages: List<EducationalStage> = emptyList(),

    @Column(name = "ordination_date", nullable = true)
    val ordinationDate: LocalDate? = null
)
