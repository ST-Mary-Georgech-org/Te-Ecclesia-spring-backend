package org.teEcclesia.identity.api.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.teEcclesia.identity.entity.VerificationMethod

data class ForgotPasswordRequest(
    @field:NotBlank(message = "Key is required")
    val key: String,

    @field:NotNull(message = "Method is required")
    val method: VerificationMethod
)