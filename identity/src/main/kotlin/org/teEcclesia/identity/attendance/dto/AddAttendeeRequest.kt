package org.teEcclesia.identity.attendance.dto

import jakarta.validation.constraints.NotBlank

data class AddAttendeeRequest(
    @field:NotBlank
    val code: String
)
