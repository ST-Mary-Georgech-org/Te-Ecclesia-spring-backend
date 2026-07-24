package org.teEcclesia.identity.api.dto.response

import java.time.LocalDate
import java.util.UUID

data class PriestResponse(
    val id: UUID,
    val name: String,
    val ordinationDate: LocalDate? = null
)
