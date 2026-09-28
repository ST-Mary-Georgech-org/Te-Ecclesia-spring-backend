package org.teEcclesia.identity.api.dto.request

import jakarta.validation.constraints.NotBlank

data class DeleteAccountRequest(
    @field:NotBlank(message = "{validation.reason.required}")
    val reason: String,

    @field:NotBlank(message = "{validation.password.required}")
    val password: String
)
