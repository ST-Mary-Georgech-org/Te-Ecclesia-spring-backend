package org.teEcclesia.identity.api.dto.response

import org.teEcclesia.identity.entity.enums.UserRole
import java.time.Instant
import java.util.UUID

data class AccountDeletionRequestResponse(
    val id: UUID,
    val userId: UUID,
    val userName: String,
    val userCode: String?,
    val userImageUrl: String?,
    val userRole: UserRole,
    val reason: String,
    val requestedAt: Instant
)
