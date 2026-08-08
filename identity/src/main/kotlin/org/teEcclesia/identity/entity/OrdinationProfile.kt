package org.teEcclesia.identity.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import org.teEcclesia.identity.entity.lookups.Rank

import org.hibernate.envers.Audited

@Audited
@Entity
@Table(name = "ordination_profiles", schema = "identity")
data class OrdinationProfile(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rank_id", nullable = false)
    val rank: Rank,

    @Column(nullable = false)
    val isOrdinationInAnotherChurch: Boolean,

    @Column(nullable = true)
    val ordinationYear: Int? = null, 

    @Column(nullable = true)
    val bishopName: String? = null, 

    @Column(nullable = true)
    val ordinationPlace: String? = null, 

    @Column(nullable = true)
    val certificateImageUrl: String? = null 
)
