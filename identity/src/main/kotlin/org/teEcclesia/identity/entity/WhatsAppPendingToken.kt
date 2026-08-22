package org.teEcclesia.identity.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.temporal.ChronoUnit

@Entity
@Table(name = "whatsapp_pending_token", schema = "identity")
data class WhatsAppPendingToken(
    @Id
    @Column(name = "user_id", nullable = false)
    val userId: String,

    @Column(nullable = false)
    val token: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant = Instant.now().plus(30, ChronoUnit.MINUTES)
) {
    fun isExpired(): Boolean {
        return Instant.now().isAfter(expiresAt)
    }
}
