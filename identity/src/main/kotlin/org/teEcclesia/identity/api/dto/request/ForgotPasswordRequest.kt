package org.teEcclesia.identity.api.dto.request

import jakarta.validation.constraints.NotBlank
import org.teEcclesia.identity.entity.VerificationMethod

data class ForgotPasswordRequest(
    @field:NotBlank(message = "{validation.key.required}")
    val key: String,

    val method: VerificationMethod
)