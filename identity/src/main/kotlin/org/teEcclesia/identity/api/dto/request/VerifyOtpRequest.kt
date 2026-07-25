package org.teEcclesia.identity.api.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.teEcclesia.identity.entity.VerificationMethod

data class VerifyOtpRequest(
    @field:NotBlank(message = "Key is required")
    val key: String,

    @field:NotBlank(message = "OTP is required")
    @field:Size(min = 4, max = 15, message = "OTP must be between 4 and 15 characters")
    val otp: String,

    val method: VerificationMethod,

    val deviceToken: String? = null
)