package org.teEcclesia.identity.api.dto.response

import java.util.UUID

data class UserSummaryResponse(
    val id: UUID,
    val code: String?,
    val name: String,
    val fullName: String,
    val imageUrl: String?
)

data class UserSummaryResponseWithFullName(
    val id: UUID,
    val code: String?,
    val fullName: String,
    val imageUrl: String?
)