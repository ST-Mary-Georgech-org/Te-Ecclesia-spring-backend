package org.teEcclesia.identity.api.dto.response

data class ForgotPasswordResponse(
    val link: String,
    val token: String
)
