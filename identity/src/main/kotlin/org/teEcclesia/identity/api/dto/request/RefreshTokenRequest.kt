package org.teEcclesia.identity.api.dto.request

import jakarta.validation.constraints.NotBlank

data class RefreshTokenRequest(

    @field:NotBlank(message = "Refresh token is required")
    val refreshToken: String,
    
    val deviceToken: String? = null
)