package org.teEcclesia.identity.api.dto.response

data class KhademProfileResponse(
    val educationalStage: LookupResponse,
    val educationalYear: LookupResponse? = null
)