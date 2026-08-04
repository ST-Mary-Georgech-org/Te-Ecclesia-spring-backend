package org.teEcclesia.identity.api.dto.response

data class LookupResponse(
    val id: Long,
    val name: String,
    val subItems: List<LookupResponse>? = null,
    val isKhademOnly: Boolean = false,
    val whatsAppLink: String? = null
)
