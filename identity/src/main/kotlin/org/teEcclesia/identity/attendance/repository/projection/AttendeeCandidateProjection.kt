package org.teEcclesia.identity.attendance.repository.projection

import org.teEcclesia.identity.entity.enums.UserRole
import java.util.UUID

interface AttendeeCandidateProjection {
    fun getId(): UUID
    fun getFirstName(): String
    fun getSecondName(): String
    fun getThirdName(): String
    fun getLastName(): String
    fun getRole(): UserRole
    fun getCode(): String?
    fun getImageUrl(): String?
    fun getMakhdoomStageId(): Long?
    fun getMakhdoomStageAr(): String?
    fun getMakhdoomStageEn(): String?
    fun getMakhdoomYearAr(): String?
    fun getMakhdoomYearEn(): String?
    fun getKhademStageId(): Long?
    fun getKhademStageAr(): String?
    fun getKhademStageEn(): String?
    fun getKhademYearAr(): String?
    fun getKhademYearEn(): String?
}
