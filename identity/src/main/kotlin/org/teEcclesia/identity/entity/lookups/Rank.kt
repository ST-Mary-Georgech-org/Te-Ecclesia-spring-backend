package org.teEcclesia.identity.entity.lookups

import jakarta.persistence.*

@Entity
@Table(name = "ranks", schema = "identity")
data class Rank(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val nameAr: String,

    @Column(nullable = false)
    val nameEn: String,

    @Column(nullable = false, length = 1)
    val codeLetter: Char,
)
