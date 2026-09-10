package org.teEcclesia.identity.repository.projection

import org.teEcclesia.identity.entity.enums.Gender
import org.teEcclesia.identity.entity.enums.UserRole
import org.teEcclesia.identity.entity.enums.UserStatus
import org.teEcclesia.identity.entity.enums.ShamamsaStudyStatus
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

interface UserProfileProjection {
    fun getId(): UUID
    fun getCode(): String?
    fun getFirstName(): String
    fun getSecondName(): String
    fun getThirdName(): String
    fun getLastName(): String
    fun getDisplayName(): String
    fun getNationalId(): String
    fun getPhone(): String
    fun getHomePhone(): String?
    fun getEmail(): String?
    fun getIsEmailVerified(): Boolean
    fun getIsPhoneVerified(): Boolean
    fun getImageUrl(): String?
    fun getJob(): String?
    fun getBuildingNo(): String
    fun getStreet(): String
    fun getStreetBranch(): String?
    fun getArea(): String
    fun getFloor(): String
    fun getApartment(): String?
    fun getSpecialMark(): String
    fun getGender(): Gender
    fun getStatus(): UserStatus
    fun getStatusReason(): String?
    fun getRole(): UserRole
    fun getCreatedAt(): Instant?
    fun getActionTakenAt(): Instant?
    fun getActionTakenBy(): UserSummaryProjection?

    fun getConfessionPriest(): UserSummaryProjection?
    fun getExternalConfessionPriestName(): String?
    fun getExternalConfessionChurch(): String?
    fun getExternalConfessionPhone(): String?

    fun getKhademProfile(): KhademProfileProjection?
    fun getKahenProfile(): KahenProfileProjection?
    fun getParentProfile(): ParentProfileProjection?
    fun getOrdinationProfile(): OrdinationProfileProjection?
    fun getMakhdoomProfile(): MakhdoomProfileProjection?
}

interface UserSummaryProjection {
    fun getId(): UUID
    fun getDisplayName(): String
    fun getFullName(): String
    fun getCode(): String?
    fun getImageUrl(): String?
}

interface StageLookupProjection {
    fun getId(): Long
    fun getNameAr(): String
    fun getNameEn(): String
}

interface YearLookupProjection {
    fun getId(): Long
    fun getNameAr(): String
    fun getNameEn(): String
    fun getWhatsAppLink(): String?
}

interface KhademProfileProjection {
    fun getId(): Long
    fun getEducationalStage(): StageLookupProjection
    fun getEducationalYear(): YearLookupProjection?
    fun getCanApproveRequests(): Boolean
    fun getResponsibleStages(): List<StageLookupProjection>
    fun getResponsibleYears(): List<YearLookupProjection>
}

interface KahenProfileProjection {
    fun getId(): Long
    fun getEducationalStages(): List<StageLookupProjection>
    fun getOrdinationDate(): LocalDate?
}

interface ParentProfileProjection {
    fun getId(): Long
    fun getPartner(): UserSummaryProjection?
    fun getNationalIdImageUrl(): String?
}

interface OrdinationProfileProjection {
    fun getId(): Long
    fun getRank(): StageLookupProjection
    fun getIsOrdinationInAnotherChurch(): Boolean
    fun getOrdinationYear(): Int?
    fun getBishopName(): String?
    fun getOrdinationPlace(): String?
    fun getCertificateImageUrl(): String?
}

interface MakhdoomProfileProjection {
    fun getId(): Long
    fun getShamamsaStudyStatus(): ShamamsaStudyStatus
    fun getEducationalStage(): StageLookupProjection
    fun getEducationalYear(): YearLookupProjection?
    fun getFatherPhone(): String?
    fun getFatherWhatsapp(): String?
    fun getMotherPhone(): String?
    fun getMotherWhatsapp(): String?
    fun getIsFatherDeceased(): Boolean
    fun getIsMotherDeceased(): Boolean
    fun getIdentityDocumentImageUrl(): String?
}

interface ParentChildProjection {
    fun getParentId(): Long
    fun getChildId(): UUID
    fun getDisplayName(): String
    fun getFullName(): String
    fun getCode(): String?
    fun getImageUrl(): String?
}
