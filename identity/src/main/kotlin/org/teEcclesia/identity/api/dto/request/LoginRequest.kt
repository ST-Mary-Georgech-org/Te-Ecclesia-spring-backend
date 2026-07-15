package org.teEcclesia.identity.api.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class LoginRequest(
    @field:NotBlank(message = "Identifier is required")
    val identifier: String,

    @field:NotBlank(message = "Password is required")
    @field:Pattern(
        regexp = """^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$""",
        message = "Password must contain at least 8 characters, one uppercase, one lowercase, one number and one special character"
    )
    val password: String,

    val deviceToken: String? = null
)