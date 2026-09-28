package org.teEcclesia.identity.api.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.teEcclesia.identity.entity.VerificationMethod

data class VerifyOtpRequest(
    @field:NotBlank(message = "{validation.key.required}")
    val key: String,

    @field:NotBlank(message = "{validation.otp.required}")
    @field:Size(min = 4, max = 15, message = "{validation.otp.size}")
    val otp: String,

    val method: VerificationMethod,

    val deviceToken: String? = null
)