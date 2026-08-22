package org.teEcclesia.identity.api.dto.request

data class PendingTokenRequest(
    val userId: String,
    val token: String?,
    val action: String?
)
