package org.teEcclesia.identity.entity

import jakarta.persistence.*
import org.teEcclesia.identity.entity.lookups.EducationalStage
import org.teEcclesia.identity.entity.lookups.EducationalYear


@Entity
@Table(name = "khadem_profiles", schema = "identity")
data class KhademProfile(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "educational_stage_id", nullable = false)
    val educationalStage: EducationalStage,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "educational_year_id", nullable = true)
    val educationalYear: EducationalYear? = null
)
