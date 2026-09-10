package org.teEcclesia.identity.api.dto.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class UpdateAcademicYearRequest(
    @field:Min(value = 2000, message = "Academic year must be greater than or equal to 2000")
    @field:Max(value = 2100, message = "Academic year must be less than or equal to 2100")
    val year: Int
)
