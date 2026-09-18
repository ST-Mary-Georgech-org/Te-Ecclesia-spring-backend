package org.teEcclesia.identity.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Entity
@Table(name = "account_verification", schema = "identity")
data class AccountVerification(
    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val otp: String,

    @Column(nullable = false)
    val sentAt: Instant = Instant.now(),

    @Column(nullable = true)
    val email: String? = null,

    @Column(nullable = true)
    val phone: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val method: VerificationMethod,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val purpose: VerificationPurpose = VerificationPurpose.REGISTER,

    @Column(name = "user_id", nullable = false)
    val userId: UUID
) {
    fun isExpired(): Boolean {
        return sentAt.plus(15, ChronoUnit.MINUTES).isBefore(Instant.now())
    }
}