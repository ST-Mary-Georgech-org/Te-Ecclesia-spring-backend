package org.teEcclesia.identity.entity

import jakarta.persistence.*
import org.teEcclesia.identity.entity.lookups.Rank

@Entity
@Table(name = "ordination_profiles", schema = "identity")
data class OrdinationProfile(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rank_id", nullable = false)
    val rank: Rank,

    @Column(nullable = false)
    val ordinationYear: Int,

    @Column(nullable = false)
    val bishopName: String,

    @Column(nullable = false)
    val ordinationPlace: String,

    @Column(nullable = true)
    val certificateImageUrl: String? = null
)
