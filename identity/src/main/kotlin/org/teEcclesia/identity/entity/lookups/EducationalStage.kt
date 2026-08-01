package org.teEcclesia.identity.entity.lookups

import jakarta.persistence.*

import org.hibernate.envers.Audited

@Audited
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

    @Column(name = "is_khadem_only", nullable = false, columnDefinition = "boolean default false")
    val isKhademOnly: Boolean = false,
    
    @OneToMany(mappedBy = "stage", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val years: MutableList<EducationalYear> = mutableListOf()
)
