package org.teEcclesia.identity.repository.projection

import org.teEcclesia.identity.entity.enums.UserRole
import org.teEcclesia.identity.entity.enums.UserStatus
import java.util.UUID

interface UserAuthDetailsProjection {
    fun getId(): UUID
    fun getFullName(): String
    fun getPasswordHash(): String
    fun getImageUrl(): String?
    fun getRole(): UserRole
    fun getStatus(): UserStatus
}
