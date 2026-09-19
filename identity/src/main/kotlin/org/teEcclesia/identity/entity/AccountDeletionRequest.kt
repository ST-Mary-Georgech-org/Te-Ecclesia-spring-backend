package org.teEcclesia.identity.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.envers.Audited
import java.time.Instant
import java.util.UUID

@Audited
@Entity
@Table(name = "account_deletion_requests", schema = "identity")
data class AccountDeletionRequest(
    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false, unique = true)
    val userId: UUID,

    @Column(nullable = false, length = 1000)
    val reason: String,

    @Column(nullable = false)
    val requestedAt: Instant = Instant.now()
)
