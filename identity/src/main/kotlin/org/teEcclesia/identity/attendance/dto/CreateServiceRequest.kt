package org.teEcclesia.identity.attendance.dto

import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class CreateServiceRequest(
    @field:NotBlank
    val name: String,
    val educationalStageId: Long?,
    val responsibleServantIds: List<UUID>
)
