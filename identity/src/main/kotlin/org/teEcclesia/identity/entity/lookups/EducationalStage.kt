package org.teEcclesia.identity.entity.lookups

import jakarta.persistence.*

@Entity
@Table(name = "educational_stages", schema = "identity")
data class EducationalStage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val nameAr: String,

    @Column(nullable = false)
    val nameEn: String,
    
    @OneToMany(mappedBy = "stage", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val years: MutableList<EducationalYear> = mutableListOf()
)
