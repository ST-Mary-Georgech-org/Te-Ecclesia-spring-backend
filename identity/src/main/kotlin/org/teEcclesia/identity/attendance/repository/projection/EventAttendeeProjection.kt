package org.teEcclesia.identity.attendance.repository.projection

import org.teEcclesia.identity.entity.enums.UserRole
import java.time.Instant
import java.util.UUID

interface EventAttendeeProjection {
    fun getId(): Long
    fun getEventId(): Long
    fun getRegisteredAt(): Instant
    fun getUserId(): UUID
    fun getFirstName(): String
    fun getSecondName(): String
    fun getThirdName(): String
    fun getLastName(): String
    fun getRole(): UserRole
    fun getMakhdoomStageAr(): String?
    fun getMakhdoomStageEn(): String?
    fun getMakhdoomYearAr(): String?
    fun getMakhdoomYearEn(): String?
    fun getKhademStageAr(): String?
    fun getKhademStageEn(): String?
    fun getKhademYearAr(): String?
    fun getKhademYearEn(): String?
}
