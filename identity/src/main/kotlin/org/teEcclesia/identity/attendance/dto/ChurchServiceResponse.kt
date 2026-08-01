package org.teEcclesia.identity.attendance.dto

import java.time.Instant

data class ChurchServiceResponse(
    val id: Long,
    val name: String,
    val createdAt: Instant
)
