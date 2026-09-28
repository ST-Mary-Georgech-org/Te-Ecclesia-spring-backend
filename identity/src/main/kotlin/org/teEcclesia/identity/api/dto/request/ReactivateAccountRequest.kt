package org.teEcclesia.identity.api.dto.request

import jakarta.validation.constraints.NotBlank

data class ReactivateAccountRequest(
    @field:NotBlank(message = "{validation.national_id.required}")
    val nationalId: String,

    @field:NotBlank(message = "{validation.password.required}")
    val password: String,

    val deviceToken: String? = null
)
