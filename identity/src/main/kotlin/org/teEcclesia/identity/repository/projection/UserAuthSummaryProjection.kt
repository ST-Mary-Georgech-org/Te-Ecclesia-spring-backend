package org.teEcclesia.identity.repository.projection

import org.teEcclesia.identity.entity.enums.UserRole
import org.teEcclesia.identity.entity.enums.UserStatus
import java.util.UUID

interface UserAuthSummaryProjection {
    fun getId(): UUID
    fun getCode(): String
    fun getPhone(): String
    fun getEmail(): String?
    fun getNationalId(): String
    fun getFirstName(): String
    fun getSecondName(): String
    fun getThirdName(): String
    fun getLastName(): String
    fun getDisplayName(): String?
    fun getPasswordHash(): String
    fun getRole(): UserRole
    fun getStatus(): UserStatus
    fun getIsEmailVerified(): Boolean
    fun getIsPhoneVerified(): Boolean
}
