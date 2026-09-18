package org.teEcclesia.identity.api.dto.request

import jakarta.validation.constraints.NotBlank

data class ReactivateAccountRequest(
    @field:NotBlank(message = "National ID is required")
    val nationalId: String,

    @field:NotBlank(message = "Password is required")
    val password: String,

    val deviceToken: String? = null
)
