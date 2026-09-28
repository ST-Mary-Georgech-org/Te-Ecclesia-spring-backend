package org.teEcclesia.identity.api.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class LoginRequest(
    @field:NotBlank(message = "{validation.identifier.required}")
    val identifier: String,

    @field:NotBlank(message = "{validation.password.required}")
    @field:Pattern(
        regexp = """^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$""",
        message = "{validation.password.pattern}"
    )
    val password: String,

    val deviceToken: String? = null
)