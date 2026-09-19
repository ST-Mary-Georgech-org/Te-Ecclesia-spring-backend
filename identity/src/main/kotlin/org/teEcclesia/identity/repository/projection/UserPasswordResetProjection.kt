package org.teEcclesia.identity.repository.projection

import java.util.UUID

interface UserPasswordResetProjection {
    fun getId(): String
    fun getPhone(): String
    fun getEmail(): String?
    fun getNationalId(): String
    fun getFullName(): String
    fun getDeleted(): Boolean
    fun getIsEmailVerified(): Boolean
}
