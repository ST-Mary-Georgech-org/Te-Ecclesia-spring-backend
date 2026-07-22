package org.teEcclesia.identity.api.dto.response

data class KahenProfileResponse(
    val educationalStages: List<LookupResponse> = emptyList()
)
