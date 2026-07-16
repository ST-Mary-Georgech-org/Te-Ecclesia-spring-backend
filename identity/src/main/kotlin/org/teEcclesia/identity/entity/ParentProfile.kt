package org.teEcclesia.identity.entity

import jakarta.persistence.*

import org.hibernate.envers.Audited

@Audited
@Entity
@Table(name = "parent_profiles", schema = "identity")
data class ParentProfile(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    val user: User,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id")
    var partner: User? = null,

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "parent_children",
        schema = "identity",
        joinColumns = [JoinColumn(name = "parent_profile_id")],
        inverseJoinColumns = [JoinColumn(name = "child_id")]
    )
    var children: MutableList<User> = mutableListOf(),

    @Column(nullable = true)
    var nationalIdImageUrl: String? = null
)