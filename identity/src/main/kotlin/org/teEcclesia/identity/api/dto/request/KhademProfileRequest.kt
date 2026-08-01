package org.teEcclesia.identity.api.dto.request

data class KhademProfileRequest(
    val educationalStageId: Long,
    val educationalYearId: Long? = null,
    val canApproveRequests: Boolean = false,
    val responsibleStageIds: List<Long> = emptyList(),
    val responsibleYearIds: List<Long> = emptyList()
)
