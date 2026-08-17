package org.teEcclesia.identity.api.dto.response

data class ParentProfileResponse(
    val partner: UserSummaryResponseWithFullName?,
    val children: List<UserSummaryResponseWithFullName>,
    val nationalIdImageUrl: String? = null,
    val whatsAppLink: String? = null
)