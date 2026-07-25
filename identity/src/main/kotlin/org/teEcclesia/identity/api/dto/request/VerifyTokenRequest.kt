package org.teEcclesia.identity.api.dto.request

data class VerifyTokenRequest(
    val token: String,
    val fromNumber: String
)
