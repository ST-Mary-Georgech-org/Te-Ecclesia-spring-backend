package org.teEcclesia.identity.api.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class UpdateProfileRequest(
    @field:NotBlank(message = "Username is required")
    val username: String,

    @field:NotBlank(message = "Full name is required")
    val fullName: String,

    @field:NotBlank(message = "Phone number is required")
    val phone: String,

    @field:Email(message = "Please provide a valid email address")
    val email: String? = null
)