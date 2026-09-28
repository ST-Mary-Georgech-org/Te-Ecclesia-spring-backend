package org.teEcclesia.identity.api.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.teEcclesia.identity.entity.VerificationMethod

data class ResetPasswordRequest(
    @field:NotBlank(message = "{validation.key.required}")
    val key: String,

    @field:NotBlank(message = "{validation.otp.required}")
    @field:Size(min = 4, max = 15, message = "{validation.otp.size}")
    val otp: String,

    @field:NotBlank(message = "{validation.new_password.required}")
    @field:Pattern(
        regexp = """^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$""",
        message = "{validation.password.pattern}"
    )
    val newPassword: String,

    val method: VerificationMethod
)