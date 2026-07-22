package org.teEcclesia.identity.api.dto.request

data class KahenProfileRequest(
    val educationalStageIds: List<Long> = emptyList()
)
