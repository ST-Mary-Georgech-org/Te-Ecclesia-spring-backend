package org.teEcclesia.identity.api.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class UpdateProfileRequest(
    @field:NotBlank(message = "{validation.first_name.required}")
    val firstName: String,

    @field:NotBlank(message = "{validation.second_name.required}")
    val secondName: String,

    @field:NotBlank(message = "{validation.third_name.required}")
    val thirdName: String,

    @field:NotBlank(message = "{validation.last_name.required}")
    val lastName: String,

    @field:NotBlank(message = "{validation.display_name.required}")
    val displayName: String,

    @field:NotBlank(message = "{validation.phone.required}")
    val phone: String,

    @field:Email(message = "{validation.email.invalid}")
    val email: String? = null
)