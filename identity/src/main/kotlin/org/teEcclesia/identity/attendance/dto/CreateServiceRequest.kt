package org.teEcclesia.identity.attendance.dto

import jakarta.validation.constraints.NotBlank

data class CreateServiceRequest(
    @field:NotBlank
    val name: String
)
