package org.teEcclesia.identity.attendance.repository.projection

import java.util.UUID

interface ResponsibleServantProjection {
    fun getServiceId(): Long
    fun getId(): UUID
    fun getFirstName(): String
    fun getSecondName(): String
    fun getThirdName(): String
    fun getLastName(): String
    fun getCode(): String?
    fun getImageUrl(): String?
}
