package org.teEcclesia.identity.repository.projection

import org.teEcclesia.identity.entity.enums.UserRole
import java.time.Instant
import java.util.UUID

interface AccountDeletionRequestProjection {
    fun getId(): UUID
    fun getUserId(): UUID
    fun getUserName(): String
    fun getUserCode(): String?
    fun getUserImageUrl(): String?
    fun getUserRole(): UserRole
    fun getReason(): String
    fun getRequestedAt(): Instant
}
