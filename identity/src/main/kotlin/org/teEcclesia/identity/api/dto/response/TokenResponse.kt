package org.teEcclesia.identity.api.dto.response

data class TokenResponse(
    val token: String,
    val refreshToken: String? = null
)
