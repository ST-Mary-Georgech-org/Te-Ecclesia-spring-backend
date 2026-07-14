package org.teEcclesia.identity.api.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class VerifyPhoneRequest(
    @field:NotBlank(message = "Phone is required")
    val phone: String,

    @field:NotBlank(message = "OTP is required")
    @field:Size(min = 4, max = 5, message = "OTP must be 4 or 5 characters")
    val otp: String,

    val deviceToken: String? = null
)
