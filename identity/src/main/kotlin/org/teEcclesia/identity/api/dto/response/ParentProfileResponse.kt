package org.teEcclesia.identity.api.dto.response

data class ParentProfileResponse(
    val partner: UserSummaryResponse?,
    val children: List<UserSummaryResponse>,
    val nationalIdImageUrl: String? = null
)