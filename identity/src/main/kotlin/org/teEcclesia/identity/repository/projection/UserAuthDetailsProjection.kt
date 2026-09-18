package org.teEcclesia.identity.repository.projection

interface UserAuthDetailsProjection {
    fun getId(): String
    fun getFullName(): String
    fun getPasswordHash(): String
    fun getImageUrl(): String?
    fun getRole(): String
    fun getStatus(): String
}
