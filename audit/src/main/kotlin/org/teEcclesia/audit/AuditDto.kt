package org.teEcclesia.audit

data class AuditDto(
    val revisionId: Int,
    val timestamp: Long,
    val operation: String,
    val entityData: Any
)
