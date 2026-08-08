package org.teEcclesia.identity.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*

import org.hibernate.envers.Audited
import org.hibernate.annotations.BatchSize

@Audited
@Entity
@BatchSize(size = 25)
@Table(name = "parent_profiles", schema = "identity")
data class ParentProfile(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    val user: User,

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id")
    val partner: User? = null,

    @BatchSize(size = 25)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "parent_children",
        schema = "identity",
        joinColumns = [JoinColumn(name = "parent_profile_id")],
        inverseJoinColumns = [JoinColumn(name = "child_id")]
    )
    val children: List<User> = emptyList(),

    @Column(nullable = true)
    val nationalIdImageUrl: String? = null
)