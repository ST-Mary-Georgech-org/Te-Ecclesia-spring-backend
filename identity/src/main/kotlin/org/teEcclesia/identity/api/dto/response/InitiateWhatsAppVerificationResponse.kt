package org.teEcclesia.identity.api.dto.response

data class InitiateWhatsAppVerificationResponse(
    val deepLink: String,
    val token: String
)
