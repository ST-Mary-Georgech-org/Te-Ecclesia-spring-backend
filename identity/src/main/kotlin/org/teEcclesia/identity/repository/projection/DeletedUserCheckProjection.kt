package org.teEcclesia.identity.repository.projection

import org.teEcclesia.identity.entity.enums.UserStatus
import java.util.UUID

interface DeletedUserCheckProjection {
    fun getId(): UUID
    fun getStatus(): UserStatus
    fun getNationalId(): String
    fun getFullName(): String
    fun getPasswordHash(): String
}
