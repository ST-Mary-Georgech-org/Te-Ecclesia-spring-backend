package org.teEcclesia.identity.api.dto.request

data class AdminKhademProfileRequest(
    val canApproveRequests: Boolean,
    val responsibleStageIds: List<Long>,
    val responsibleYearIds: List<Long>
)
