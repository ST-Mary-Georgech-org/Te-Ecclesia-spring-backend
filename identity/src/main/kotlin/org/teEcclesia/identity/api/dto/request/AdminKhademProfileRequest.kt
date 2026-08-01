package org.teEcclesia.identity.api.dto.request

data class AdminKhademProfileRequest(
    val canApproveRequests: Boolean? = false,
    val responsibleStageIds: List<Long>? = emptyList(),
    val responsibleYearIds: List<Long>? = emptyList()
)
