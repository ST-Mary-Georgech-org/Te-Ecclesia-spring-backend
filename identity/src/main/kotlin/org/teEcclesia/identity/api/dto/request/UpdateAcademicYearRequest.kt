package org.teEcclesia.identity.api.dto.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class UpdateAcademicYearRequest(
    @field:Min(value = 2000, message = "{validation.academic_year.min}")
    @field:Max(value = 2100, message = "{validation.academic_year.max}")
    val year: Int
)
