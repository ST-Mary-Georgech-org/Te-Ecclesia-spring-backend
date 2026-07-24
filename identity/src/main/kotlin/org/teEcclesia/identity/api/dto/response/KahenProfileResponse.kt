package org.teEcclesia.identity.api.dto.response

import java.time.LocalDate

data class KahenProfileResponse(
    val educationalStages: List<LookupResponse> = emptyList(),
    val ordinationDate: LocalDate? = null
)
