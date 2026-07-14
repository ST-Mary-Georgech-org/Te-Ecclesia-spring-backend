package org.teEcclesia.identity.api.dto.request

data class WhatsAppWebhookMessage(
    val from: String,
    val body: String
)
