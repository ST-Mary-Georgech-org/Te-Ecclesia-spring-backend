package org.teEcclesia.identity.api.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class UpdateProfileRequest(
    @field:NotBlank(message = "First name is required")
    val firstName: String,

    @field:NotBlank(message = "Second name is required")
    val secondName: String,

    @field:NotBlank(message = "Third name is required")
    val thirdName: String,

    @field:NotBlank(message = "Last name is required")
    val lastName: String,

    @field:NotBlank(message = "Display name is required")
    val displayName: String,

    @field:NotBlank(message = "Phone number is required")
    val phone: String,

    @field:Email(message = "Please provide a valid email address")
    val email: String? = null
)