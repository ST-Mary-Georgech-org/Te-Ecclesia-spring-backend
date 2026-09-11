package org.teEcclesia.identity.attendance.dto

import java.util.UUID

data class ResponsibleServantDto(
    val id: UUID,
    val name: String,
    val code: String?,
    val imageUrl: String?
)
