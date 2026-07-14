package org.teEcclesia.identity.entity

import jakarta.persistence.*
import org.teEcclesia.events.identity.UserCreatedEvent
import org.teEcclesia.events.identity.UserUpdatedEvent
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "users", schema = "identity")
data class User(
    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true)
    val username: String,

    @Column(nullable = false)
    val fullName: String,

    @Column(nullable = false, unique = true)
    val phone: String,

    @Column(nullable = true, unique = true)
    val email: String? = null,

    @Column(nullable = false)
    val passwordHash: String,

    @Column(nullable = true)
    val imageUrl: String? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(nullable = false)
    val isEmailVerified: Boolean = false,

    @Column(nullable = false)
    val isPhoneVerified: Boolean = false,

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val accountVerifications: MutableList<AccountVerification> = mutableListOf(),

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    val refreshTokens: MutableList<RefreshToken> = mutableListOf()
)

fun User.toUserCreatedEvent(): UserCreatedEvent {
    return UserCreatedEvent(
        id = id,
        password = passwordHash,
        fullName = fullName,
        imageUrl = imageUrl,
    )
}

fun User.toUserUpdatedEvent(): UserUpdatedEvent {
    return UserUpdatedEvent(
        id = id,
        password = passwordHash,
        fullName = fullName,
        imageUrl = imageUrl,
    )
}
