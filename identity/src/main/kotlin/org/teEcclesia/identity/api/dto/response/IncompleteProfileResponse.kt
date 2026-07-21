package org.teEcclesia.identity.api.dto.response

data class IncompleteProfileResponse(
    val message: String,
    val status: Int,
    val token: String? = null
)
