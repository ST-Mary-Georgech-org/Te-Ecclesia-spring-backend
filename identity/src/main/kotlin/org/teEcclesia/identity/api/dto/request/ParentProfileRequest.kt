package org.teEcclesia.identity.api.dto.request

data class ParentProfileRequest(
    val partnerCode: String? = null,
    val childrenCodes: List<String>? = emptyList()
)
