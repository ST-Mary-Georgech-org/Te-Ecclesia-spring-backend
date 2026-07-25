package org.teEcclesia.identity.api.dto.request

import java.time.LocalDate

data class KahenProfileRequest(
    val educationalStageIds: List<Long> = emptyList(),
    val ordinationDate: LocalDate? = null
)
