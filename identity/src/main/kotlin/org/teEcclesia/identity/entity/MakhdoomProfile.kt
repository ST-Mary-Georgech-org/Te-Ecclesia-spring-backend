package org.teEcclesia.identity.entity

import jakarta.persistence.*
import org.teEcclesia.identity.entity.enums.ShamamsaStudyStatus
import org.teEcclesia.identity.entity.lookups.EducationalStage
import org.teEcclesia.identity.entity.lookups.EducationalYear

import org.hibernate.envers.Audited

@Audited
@Entity
@Table(name = "makhdoom_profiles", schema = "identity")
data class MakhdoomProfile(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    val user: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val shamamsaStudyStatus: ShamamsaStudyStatus,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "educational_stage_id", nullable = false)
    val educationalStage: EducationalStage,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "educational_year_id", nullable = true)
    val educationalYear: EducationalYear? = null,

    @Column(nullable = true)
    val fatherPhone: String? = null,

    @Column(nullable = true)
    val fatherWhatsapp: String? = null,

    @Column(nullable = true)
    val motherPhone: String? = null,

    @Column(nullable = true)
    val motherWhatsapp: String? = null,

    @Column(nullable = false)
    val isFatherDeceased: Boolean = false,

    @Column(nullable = false)
    val isMotherDeceased: Boolean = false,

    @Column(nullable = true)
    var identityDocumentImageUrl: String? = null
)
