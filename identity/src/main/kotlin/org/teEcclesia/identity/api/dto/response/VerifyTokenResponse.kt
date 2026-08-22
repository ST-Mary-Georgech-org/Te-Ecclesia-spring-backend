package org.teEcclesia.identity.api.dto.response

data class VerifyTokenResponse(
    val verified: Boolean,
    val message: String
)
