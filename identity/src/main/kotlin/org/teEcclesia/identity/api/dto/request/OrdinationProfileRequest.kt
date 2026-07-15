package org.teEcclesia.identity.api.dto.request

import jakarta.validation.constraints.NotBlank

data class OrdinationProfileRequest(
    val rankId: Long,
    
    val ordinationYear: Int,
    
    @field:NotBlank(message = "Bishop name is required")
    val bishopName: String,
    
    val ordinationPlace: String,
    
    val certificateImageUrl: String? = null
)
