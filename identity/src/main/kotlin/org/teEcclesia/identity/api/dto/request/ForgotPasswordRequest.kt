package org.teEcclesia.identity.api.dto.request

import jakarta.validation.constraints.NotBlank
import org.teEcclesia.identity.entity.VerificationMethod

data class ForgotPasswordRequest(
    @field:NotBlank(message = "Key is required")
    val key: String,

    val method: VerificationMethod
)