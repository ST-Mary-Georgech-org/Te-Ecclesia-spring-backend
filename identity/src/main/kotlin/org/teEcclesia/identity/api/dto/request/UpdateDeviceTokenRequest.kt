package org.teEcclesia.identity.api.dto.request

import jakarta.validation.constraints.NotBlank

data class UpdateDeviceTokenRequest(
    @field:NotBlank(message = "Refresh token is required")
    val refreshToken: String,

    @field:NotBlank(message = "Device token is required")
    val deviceToken: String
)
