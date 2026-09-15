package org.teEcclesia.identity.repository.projection

import java.util.UUID

interface UserEmailProjection {
    fun getId(): UUID
    fun getEmail(): String?
}
