package org.teEcclesia.identity.api.dto.response

data class RegisterResponse(
    val message: String,
    val whatsappDeepLink: String,
    val token: String
)
