package org.teEcclesia.identity.api.dto.request

import jakarta.validation.constraints.NotBlank

data class DeleteAccountRequest(
    @field:NotBlank(message = "Reason is required")
    val reason: String,

    @field:NotBlank(message = "Password is required")
    val password: String
)
