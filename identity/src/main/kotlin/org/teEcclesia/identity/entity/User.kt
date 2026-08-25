package org.teEcclesia.identity.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import org.teEcclesia.events.identity.UserCreatedEvent
import org.teEcclesia.events.identity.UserUpdatedEvent
import org.teEcclesia.identity.entity.enums.Gender
import org.teEcclesia.identity.entity.enums.UserRole
import org.teEcclesia.identity.entity.enums.UserStatus
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

import org.hibernate.envers.Audited
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.SQLRestriction

@Audited
@Entity
@Table(name = "users", schema = "identity")
@SQLDelete(sql = "UPDATE identity.users SET deleted = true WHERE id=?")
@SQLRestriction("deleted = false")
data class User(
    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val firstName: String,

    @Column(nullable = false)
    val secondName: String,

    @Column(nullable = false)
    val thirdName: String,

    @Column(nullable = false)
    val lastName: String,

    @Column(nullable = false)
    val displayName: String,

    @Column(nullable = false, length = 14)
    val nationalId: String,

    @Column(nullable = false)
    val phone: String,

    @Column(nullable = true)
    val homePhone: String? = null,

    @Column(nullable = true)
    val email: String? = null,

    @Column(nullable = false)
    val passwordHash: String,

    @Column(nullable = true)
    val imageUrl: String? = null,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(nullable = false)
    val birthDate: LocalDate,

    @Column(nullable = true)
    val job: String? = null,

    @Column(nullable = false)
    val buildingNo: String, 

    @Column(nullable = false)
    val street: String,

    @Column(nullable = true)
    val streetBranch: String? = null,

    @Column(nullable = false)
    val area: String,

    @Column(nullable = false)
    val floor: String,

    @Column(nullable = true)
    val apartment: String? = null,

    @Column(nullable = false)
    val specialMark: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val gender: Gender,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: UserStatus = UserStatus.UNVERIFIED,

    @Column(nullable = true)
    val statusReason: String? = null,

    @Column(nullable = true)
    val actionTakenAt: Instant? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: UserRole,

    @Column(nullable = true, length = 9)
    val code: String? = null,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confession_priest_id")
    val confessionPriest: User? = null,

    @Column(nullable = true)
    val externalConfessionPriestName: String? = null,

    @Column(nullable = true)
    val externalConfessionChurch: String? = null,

    @Column(nullable = true)
    val externalConfessionPhone: String? = null,

    @Column(nullable = false)
    val isEmailVerified: Boolean = false,

    @Column(nullable = false)
    val isPhoneVerified: Boolean = false,

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val accountVerifications: MutableList<AccountVerification> = mutableListOf(),

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    val refreshTokens: MutableList<RefreshToken> = mutableListOf(),

    @OneToOne(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val ordinationProfile: OrdinationProfile? = null,

    @OneToOne(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val makhdoomProfile: MakhdoomProfile? = null,

    @OneToOne(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val khademProfile: KhademProfile? = null,

    @OneToOne(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val kahenProfile: KahenProfile? = null,

    @OneToOne(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val parentProfile: ParentProfile? = null,

    @Column(nullable = false)
    val deleted: Boolean = false
) {
    val fullName: String
        get() = "$firstName $secondName $thirdName $lastName"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "User(id=$id, nationalId='$nationalId', phone='$phone', role=$role, status=$status)"
    }
}

fun User.toUserCreatedEvent(): UserCreatedEvent {
    return UserCreatedEvent(
        id = id,
        password = passwordHash,
        fullName = this.fullName,
        imageUrl = imageUrl,
    )
}

fun User.toUserUpdatedEvent(): UserUpdatedEvent {
    return UserUpdatedEvent(
        id = id,
        password = passwordHash,
        fullName = this.fullName,
        imageUrl = imageUrl,
    )
}
