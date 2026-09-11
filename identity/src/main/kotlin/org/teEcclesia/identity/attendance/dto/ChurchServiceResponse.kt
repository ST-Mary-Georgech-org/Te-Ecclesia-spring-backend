package org.teEcclesia.identity.attendance.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class ChurchServiceResponse(
    val id: Long,
    val name: String,
    val createdAt: Instant,
    val responsible: Boolean,
    val educationalStageId: Long?,
    val educationalStageName: String?,
    val responsibleServants: List<ResponsibleServantDto>
)
