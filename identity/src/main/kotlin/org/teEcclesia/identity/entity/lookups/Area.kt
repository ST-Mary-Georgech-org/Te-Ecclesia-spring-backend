package org.teEcclesia.identity.entity.lookups

import jakarta.persistence.*

@Entity
@Table(name = "areas", schema = "identity")
data class Area(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val name: String,

    @Column(nullable = false)
    val suggestedCount: Int = 1
)
