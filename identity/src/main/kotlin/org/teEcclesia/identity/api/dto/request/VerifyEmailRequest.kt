package org.teEcclesia.identity.api.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class VerifyEmailRequest(
    @field:NotBlank(message = "{validation.email.required}")
    @field:Email(message = "{validation.email.invalid}")
    val email: String,

    @field:NotBlank(message = "{validation.otp.required}")
    @field:Size(min = 4, max = 5, message = "{validation.otp.size_email}")
    val otp: String,

    val deviceToken: String? = null
)
