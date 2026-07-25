package org.teEcclesia.identity.api.dto.response

data class OrdinationProfileResponse(
    val rank: LookupResponse,
    val isOrdinationInAnotherChurch: Boolean,
    val ordinationYear: Int? = null,
    val bishopName: String? = null,
    val ordinationPlace: String? = null,
    val certificateImageUrl: String? = null
)
