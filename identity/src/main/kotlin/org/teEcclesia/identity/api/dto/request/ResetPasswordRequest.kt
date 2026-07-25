package org.teEcclesia.identity.api.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.teEcclesia.identity.entity.VerificationMethod

data class ResetPasswordRequest(
    @field:NotBlank(message = "Key is required")
    val key: String,

    @field:NotBlank(message = "OTP is required")
    @field:Size(min = 4, max = 15, message = "OTP must be between 4 and 15 characters")
    val otp: String,

    @field:NotBlank(message = "New password is required")
    @field:Pattern(
        regexp = """^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$""",
        message = "Password must contain at least 8 characters, one uppercase, one lowercase, one number and one special character"
    )
    val newPassword: String,

    val method: VerificationMethod
)