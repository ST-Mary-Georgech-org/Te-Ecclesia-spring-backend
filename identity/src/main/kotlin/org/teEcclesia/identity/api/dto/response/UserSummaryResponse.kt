package org.teEcclesia.identity.api.dto.response

data class UserSummaryResponse(
    val id: String,
    val fullName: String,
    val code: String?,
    val imageUrl: String?
)