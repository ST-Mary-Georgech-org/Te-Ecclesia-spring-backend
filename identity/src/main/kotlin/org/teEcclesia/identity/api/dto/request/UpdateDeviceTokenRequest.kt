package org.teEcclesia.identity.api.dto.request

import jakarta.validation.constraints.NotBlank

data class UpdateDeviceTokenRequest(
    @field:NotBlank(message = "{validation.refresh_token.required}")
    val refreshToken: String,

    @field:NotBlank(message = "{validation.device_token.required}")
    val deviceToken: String
)
