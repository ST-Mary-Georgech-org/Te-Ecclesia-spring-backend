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
    // Action Taken By (Flat Summary)
    fun getActionTakenById(): UUID?
    fun getActionTakenByName(): String?
    fun getActionTakenByFullName(): String?
    fun getActionTakenByCode(): String?
    fun getActionTakenByImageUrl(): String?

    // Confession Priest (Flat Summary)
    fun getConfessionPriestId(): UUID?
    fun getConfessionPriestName(): String?
    fun getConfessionPriestFullName(): String?
    fun getConfessionPriestCode(): String?
    fun getConfessionPriestImageUrl(): String?
    fun getExternalConfessionPriestName(): String?
    fun getExternalConfessionChurch(): String?
    fun getExternalConfessionPhone(): String?

    // Khadem Profile
    fun getKhademProfileId(): Long?
    fun getKhademCanApproveRequests(): Boolean?
    fun getKhademStageId(): Long?
    fun getKhademStageNameAr(): String?
    fun getKhademStageNameEn(): String?
    fun getKhademYearId(): Long?
    fun getKhademYearNameAr(): String?
    fun getKhademYearNameEn(): String?
    fun getKhademYearWhatsAppLink(): String?

    // Kahen Profile
    fun getKahenProfileId(): Long?
    fun getKahenOrdinationDate(): LocalDate?

    // Parent Profile & Partner
    fun getParentProfileId(): Long?
    fun getParentNationalIdImageUrl(): String?
    fun getPartnerId(): UUID?
    fun getPartnerName(): String?
    fun getPartnerFullName(): String?
    fun getPartnerCode(): String?
    fun getPartnerImageUrl(): String?

    // Ordination Profile
    fun getOrdinationProfileId(): Long?
    fun getOrdinationRankId(): Long?
    fun getOrdinationRankNameAr(): String?
    fun getOrdinationRankNameEn(): String?
    fun getOrdinationIsOrdinationInAnotherChurch(): Boolean?
    fun getOrdinationYear(): Int?
    fun getOrdinationBishopName(): String?
    fun getOrdinationPlace(): String?
    fun getOrdinationCertificateImageUrl(): String?

    // Makhdoom Profile
    fun getMakhdoomProfileId(): Long?
    fun getMakhdoomShamamsaStudyStatus(): ShamamsaStudyStatus?
    fun getMakhdoomStageId(): Long?
    fun getMakhdoomStageNameAr(): String?
    fun getMakhdoomStageNameEn(): String?
    fun getMakhdoomYearId(): Long?
    fun getMakhdoomYearNameAr(): String?
    fun getMakhdoomYearNameEn(): String?
    fun getMakhdoomYearWhatsAppLink(): String?
    fun getMakhdoomFatherPhone(): String?
    fun getMakhdoomFatherWhatsapp(): String?
    fun getMakhdoomMotherPhone(): String?
    fun getMakhdoomMotherWhatsapp(): String?
    fun getMakhdoomIsFatherDeceased(): Boolean?
    fun getMakhdoomIsMotherDeceased(): Boolean?
    fun getMakhdoomIdentityDocumentImageUrl(): String?
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

interface ProfileStageLookupProjection {
    fun getProfileId(): Long
    fun getId(): Long
    fun getNameAr(): String
    fun getNameEn(): String
}

interface ProfileYearLookupProjection {
    fun getProfileId(): Long
    fun getId(): Long
    fun getNameAr(): String
    fun getNameEn(): String
    fun getWhatsAppLink(): String?
}

interface PriestSummaryProjection {
    fun getId(): UUID
    fun getName(): String
    fun getOrdinationDate(): LocalDate?
}

