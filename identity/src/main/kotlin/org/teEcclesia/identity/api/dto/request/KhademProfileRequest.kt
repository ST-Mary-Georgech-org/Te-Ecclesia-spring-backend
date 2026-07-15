package org.teEcclesia.identity.api.dto.request

import java.util.UUID

data class KhademProfileRequest(
    val educationalStageId: Long,
    val educationalYearId: Long? = null
)
