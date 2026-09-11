package org.teEcclesia.identity.attendance.dto

import com.fasterxml.jackson.annotation.JsonProperty
import org.teEcclesia.identity.api.dto.response.LookupResponse
import java.time.Instant

data class ChurchServiceResponse(
    val id: Long,
    val name: String,
    val createdAt: Instant,
    val responsible: Boolean,
    val educationalStages: List<LookupResponse> = emptyList(),
    val responsibleServants: List<ResponsibleServantDto>
)
