package org.teEcclesia.identity.api.dto.response

import org.springframework.context.i18n.LocaleContextHolder
import org.teEcclesia.identity.entity.User
import org.teEcclesia.identity.repository.projection.*
import java.time.Instant
import org.teEcclesia.identity.entity.enums.Gender
import org.teEcclesia.identity.entity.enums.UserStatus
import org.teEcclesia.identity.entity.enums.UserRole

data class ProfileResponse(
    val id: String,
    val code: String? = null,
    val firstName: String,
    val secondName: String,
    val thirdName: String,
    val lastName: String,
    val displayName: String,
    val fullName: String,
    val nationalId: String,
    val phone: String,
    val homePhone: String?,
    val email: String?,
    val isEmailVerified: Boolean,
    val isPhoneVerified: Boolean,
    val imageUrl: String?,
    val identityDocumentImageUrl: String? = null,
    val job: String?,
    val buildingNo: String,
    val street: String,
    val streetBranch: String?,
    val area: String,
    val floor: String,
    val apartment: String?,
    val specialMark: String,
    val gender: Gender,
    val status: UserStatus,
    val statusReason: String?,
    val role: UserRole,
    val confessionPriest: UserSummaryResponse? = null,
    val externalConfessionPriestName: String? = null,
    val externalConfessionChurch: String? = null,
    val externalConfessionPhone: String? = null,
    val khademProfile: KhademProfileResponse? = null,
    val kahenProfile: KahenProfileResponse? = null,
    val parentProfile: ParentProfileResponse? = null,
    val ordinationProfile: OrdinationProfileResponse? = null,
    val makhdoomProfile: MakhdoomProfileResponse? = null,
    val createdAt: Instant? = null,
    val actionTakenAt: Instant? = null,
    val actionTakenBy: UserSummaryResponse? = null,
    val deaconsSchoolRecord: DeaconsSchoolRecordResponse? = null
)

private fun resolveUrl(imageBaseUrl: String, relativePath: String?): String? {
    if (relativePath.isNullOrBlank()) return null
    if (relativePath.startsWith("http://") || relativePath.startsWith("https://")) return relativePath
    return if (relativePath.contains("/")) {
        val cdnBaseUrl = imageBaseUrl.substringBeforeLast('/')
        "$cdnBaseUrl/$relativePath"
    } else {
        "$imageBaseUrl/$relativePath"
    }
}

fun User.toUserSummaryResponse(imageBaseUrl: String): UserSummaryResponse {
    return UserSummaryResponse(
        id = id,
        name = displayName,
        code = code,
        fullName = fullName,
        imageUrl = resolveUrl(imageBaseUrl, imageUrl)
    )
}


fun User.toProfileResponse(
    imageBaseUrl: String, 
    parentsWhatsAppLink: String? = null,
    deaconsSchoolRecord: DeaconsSchoolRecordResponse? = null
): ProfileResponse {
    val lang = LocaleContextHolder.getLocale().language
    return ProfileResponse(
        id = id.toString(),
        code = this.code,
        firstName = firstName,
        secondName = secondName,
        thirdName = thirdName,
        lastName = lastName,
        displayName = displayName,
        fullName = fullName,
        nationalId = nationalId,
        phone = phone,
        homePhone = homePhone,
        email = email,
        isEmailVerified = isEmailVerified,
        isPhoneVerified = isPhoneVerified,
        imageUrl = resolveUrl(imageBaseUrl, imageUrl),
        identityDocumentImageUrl = resolveUrl(imageBaseUrl, this.identityDocumentImageUrl),
        job = job,
        buildingNo = buildingNo,
        street = street,
        streetBranch = streetBranch,
        area = area,
        floor = floor,
        apartment = apartment,
        specialMark = this.specialMark,
        gender = this.gender,
        status = this.status,
        statusReason = this.statusReason,
        role = this.role,
        confessionPriest = this.confessionPriest?.toUserSummaryResponse(imageBaseUrl),
        externalConfessionPriestName = this.externalConfessionPriestName,
        externalConfessionChurch = this.externalConfessionChurch,
        externalConfessionPhone = this.externalConfessionPhone,
        khademProfile = if (this.role == UserRole.KHADEM) this.khademProfile?.let {
            val stageName = if (lang.startsWith("en", ignoreCase = true)) it.educationalStage.nameEn else it.educationalStage.nameAr
            KhademProfileResponse(
                educationalStage = LookupResponse(it.educationalStage.id, stageName, whatsAppLink = null),
                educationalYear = it.educationalYear?.let { year -> 
                    val yearName = if (lang.startsWith("en", ignoreCase = true)) year.nameEn else year.nameAr
                    LookupResponse(year.id, yearName, whatsAppLink = year.whatsAppLink)
                },
                canApproveRequests = it.canApproveRequests,
                responsibleStages = it.responsibleStages.map { stage ->
                    val sName = if (lang.startsWith("en", ignoreCase = true)) stage.nameEn else stage.nameAr
                    LookupResponse(stage.id, sName, whatsAppLink = null)
                },
                responsibleYears = it.responsibleYears.map { year ->
                    val yName = if (lang.startsWith("en", ignoreCase = true)) year.nameEn else year.nameAr
                    LookupResponse(year.id, yName, whatsAppLink = year.whatsAppLink)
                }
            )
        } else null,
        kahenProfile = if (this.role == UserRole.KAHEN) this.kahenProfile?.let {
            KahenProfileResponse(
                educationalStages = it.educationalStages.map { stage ->
                    val stageName = if (lang.startsWith("en", ignoreCase = true)) stage.nameEn else stage.nameAr
                    LookupResponse(stage.id, stageName, whatsAppLink = null)
                },
                ordinationDate = it.ordinationDate
            )
        } else null,
        parentProfile = if (this.role == UserRole.PARENT) this.parentProfile?.let {
            ParentProfileResponse(
                partner = it.partner?.toUserSummaryResponse(imageBaseUrl),
                children = it.children.map { child -> child.toUserSummaryResponse(imageBaseUrl) },
                whatsAppLink = parentsWhatsAppLink
            )
        } else null,
        ordinationProfile = this.ordinationProfile?.let {
            val rankName = if (lang.startsWith("en", ignoreCase = true)) it.rank.nameEn else it.rank.nameAr
            OrdinationProfileResponse(
                rank = LookupResponse(it.rank.id, rankName, whatsAppLink = null),
                isOrdinationInAnotherChurch = it.isOrdinationInAnotherChurch,
                ordinationYear = it.ordinationYear,
                bishopName = it.bishopName,
                ordinationPlace = it.ordinationPlace,
                certificateImageUrl = resolveUrl(imageBaseUrl, it.certificateImageUrl)
            )
        },
        makhdoomProfile = if (this.role == UserRole.MAKHDOOM) this.makhdoomProfile?.let {
            val stageName = if (lang.startsWith("en", ignoreCase = true)) it.educationalStage.nameEn else it.educationalStage.nameAr
            MakhdoomProfileResponse(
                shamamsaStudyStatus = it.shamamsaStudyStatus,
                educationalStage = LookupResponse(it.educationalStage.id, stageName, whatsAppLink = null),
                educationalYear = it.educationalYear?.let { year ->
                    val yearName = if (lang.startsWith("en", ignoreCase = true)) year.nameEn else year.nameAr
                    LookupResponse(year.id, yearName, whatsAppLink = year.whatsAppLink)
                },
                fatherPhone = it.fatherPhone,
                fatherWhatsapp = it.fatherWhatsapp,
                motherPhone = it.motherPhone,
                motherWhatsapp = it.motherWhatsapp,
                isFatherDeceased = it.isFatherDeceased,
                isMotherDeceased = it.isMotherDeceased
            )
        } else null,
        createdAt = this.createdAt,
        actionTakenAt = this.actionTakenAt,
        actionTakenBy = this.actionTakenBy?.toUserSummaryResponse(imageBaseUrl),
        deaconsSchoolRecord = deaconsSchoolRecord
    )
}

fun UserProfileProjection.toProfileResponse(
    imageBaseUrl: String, 
    parentsWhatsAppLink: String?,
    childrenByParentId: Map<Long, List<UserSummaryResponse>> = emptyMap(),
    responsibleStagesByKhademId: Map<Long, List<LookupResponse>> = emptyMap(),
    responsibleYearsByKhademId: Map<Long, List<LookupResponse>> = emptyMap(),
    educationalStagesByKahenId: Map<Long, List<LookupResponse>> = emptyMap(),
    deaconsSchoolRecord: DeaconsSchoolRecordResponse? = null
): ProfileResponse {
    val lang = LocaleContextHolder.getLocale().language

    return ProfileResponse(
        id = getId().toString(),
        code = getCode(),
        firstName = getFirstName(),
        secondName = getSecondName(),
        thirdName = getThirdName(),
        lastName = getLastName(),
        displayName = getDisplayName(),
        fullName = "${getFirstName()} ${getSecondName()} ${getThirdName()} ${getLastName()}".trim(),
        nationalId = getNationalId(),
        phone = getPhone(),
        homePhone = getHomePhone(),
        email = getEmail(),
        isEmailVerified = getIsEmailVerified(),
        isPhoneVerified = getIsPhoneVerified(),
        imageUrl = resolveUrl(imageBaseUrl, getImageUrl()),
        identityDocumentImageUrl = resolveUrl(imageBaseUrl, getIdentityDocumentImageUrl()),
        job = getJob(),
        buildingNo = getBuildingNo(),
        street = getStreet(),
        streetBranch = getStreetBranch(),
        area = getArea(),
        floor = getFloor(),
        apartment = getApartment(),
        specialMark = getSpecialMark(),
        gender = getGender(),
        status = getStatus(),
        statusReason = getStatusReason(),
        role = getRole(),
        confessionPriest = getConfessionPriestId()?.let {
            UserSummaryResponse(
                id = it,
                name = getConfessionPriestName() ?: "",
                fullName = getConfessionPriestFullName() ?: "",
                code = getConfessionPriestCode(),
                imageUrl = resolveUrl(imageBaseUrl, getConfessionPriestImageUrl())
            )
        },
        externalConfessionPriestName = getExternalConfessionPriestName(),
        externalConfessionChurch = getExternalConfessionChurch(),
        externalConfessionPhone = getExternalConfessionPhone(),
        khademProfile = if (getRole() == UserRole.KHADEM) getKhademProfileId()?.let { khId ->
            val stageName = if (lang.startsWith("en", ignoreCase = true)) getKhademStageNameEn() else getKhademStageNameAr()
            val yearName = if (lang.startsWith("en", ignoreCase = true)) getKhademYearNameEn() else getKhademYearNameAr()
            KhademProfileResponse(
                educationalStage = LookupResponse(getKhademStageId() ?: 0L, stageName ?: "", whatsAppLink = null),
                educationalYear = getKhademYearId()?.let { yId ->
                    LookupResponse(yId, yearName ?: "", whatsAppLink = getKhademYearWhatsAppLink())
                },
                canApproveRequests = getKhademCanApproveRequests() ?: false,
                responsibleStages = responsibleStagesByKhademId[khId] ?: emptyList(),
                responsibleYears = responsibleYearsByKhademId[khId] ?: emptyList()
            )
        } else null,
        kahenProfile = if (getRole() == UserRole.KAHEN) getKahenProfileId()?.let { kahenId ->
            KahenProfileResponse(
                educationalStages = educationalStagesByKahenId[kahenId] ?: emptyList(),
                ordinationDate = getKahenOrdinationDate()
            )
        } else null,
        parentProfile = if (getRole() == UserRole.PARENT) getParentProfileId()?.let { parentId ->
            val partnerSummary = getPartnerId()?.let {
                UserSummaryResponse(
                    id = it,
                    name = getPartnerName() ?: "",
                    fullName = getPartnerFullName() ?: "",
                    code = getPartnerCode(),
                    imageUrl = resolveUrl(imageBaseUrl, getPartnerImageUrl())
                )
            }
            ParentProfileResponse(
                partner = partnerSummary,
                children = childrenByParentId[parentId] ?: emptyList(),
                whatsAppLink = parentsWhatsAppLink
            )
        } else null,
        ordinationProfile = getOrdinationProfileId()?.let {
            val rankName = if (lang.startsWith("en", ignoreCase = true)) getOrdinationRankNameEn() else getOrdinationRankNameAr()
            OrdinationProfileResponse(
                rank = LookupResponse(getOrdinationRankId() ?: 0L, rankName ?: "", whatsAppLink = null),
                isOrdinationInAnotherChurch = getOrdinationIsOrdinationInAnotherChurch() ?: false,
                ordinationYear = getOrdinationYear(),
                bishopName = getOrdinationBishopName(),
                ordinationPlace = getOrdinationPlace(),
                certificateImageUrl = resolveUrl(imageBaseUrl, getOrdinationCertificateImageUrl())
            )
        },
        makhdoomProfile = if (getRole() == UserRole.MAKHDOOM) getMakhdoomProfileId()?.let {
            val stageName = if (lang.startsWith("en", ignoreCase = true)) getMakhdoomStageNameEn() else getMakhdoomStageNameAr()
            val yearName = if (lang.startsWith("en", ignoreCase = true)) getMakhdoomYearNameEn() else getMakhdoomYearNameAr()
            MakhdoomProfileResponse(
                shamamsaStudyStatus = getMakhdoomShamamsaStudyStatus() ?: org.teEcclesia.identity.entity.enums.ShamamsaStudyStatus.NO,
                educationalStage = LookupResponse(getMakhdoomStageId() ?: 0L, stageName ?: "", whatsAppLink = null),
                educationalYear = getMakhdoomYearId()?.let { yId ->
                    LookupResponse(yId, yearName ?: "", whatsAppLink = getMakhdoomYearWhatsAppLink())
                },
                fatherPhone = getMakhdoomFatherPhone(),
                fatherWhatsapp = getMakhdoomFatherWhatsapp(),
                motherPhone = getMakhdoomMotherPhone(),
                motherWhatsapp = getMakhdoomMotherWhatsapp(),
                isFatherDeceased = getMakhdoomIsFatherDeceased() ?: false,
                isMotherDeceased = getMakhdoomIsMotherDeceased() ?: false
            )
        } else null,
        createdAt = getCreatedAt(),
        actionTakenAt = getActionTakenAt(),
        actionTakenBy = getActionTakenById()?.let {
            UserSummaryResponse(
                id = it,
                name = getActionTakenByName() ?: "",
                fullName = getActionTakenByFullName() ?: "",
                code = getActionTakenByCode(),
                imageUrl = resolveUrl(imageBaseUrl, getActionTakenByImageUrl())
            )
        },
        deaconsSchoolRecord = deaconsSchoolRecord
    )
}

private fun UserSummaryProjection.toUserSummaryResponse(imageBaseUrl: String): UserSummaryResponse {
    return UserSummaryResponse(
        id = getId(),
        name = getDisplayName(),
        fullName = getFullName(),
        code = getCode(),
        imageUrl = resolveUrl(imageBaseUrl, getImageUrl())
    )
}


fun ParentChildProjection.toUserSummaryResponse(imageBaseUrl: String): UserSummaryResponse {
    return UserSummaryResponse(
        id = getChildId(),
        fullName = getFullName(),
        name = getDisplayName(),
        code = getCode(),
        imageUrl = resolveUrl(imageBaseUrl, getImageUrl())
    )
}

private fun StageLookupProjection.toLookupResponse(lang: String): LookupResponse {
    val name = if (lang.startsWith("en", ignoreCase = true)) getNameEn() else getNameAr()
    return LookupResponse(getId(), name, whatsAppLink = null)
}

private fun YearLookupProjection.toLookupResponse(lang: String): LookupResponse {
    val name = if (lang.startsWith("en", ignoreCase = true)) getNameEn() else getNameAr()
    return LookupResponse(getId(), name, whatsAppLink = getWhatsAppLink())
}

fun ProfileStageLookupProjection.toLookupResponse(lang: String): LookupResponse {
    val name = if (lang.startsWith("en", ignoreCase = true)) getNameEn() else getNameAr()
    return LookupResponse(getId(), name, whatsAppLink = null)
}

fun ProfileYearLookupProjection.toLookupResponse(lang: String): LookupResponse {
    val name = if (lang.startsWith("en", ignoreCase = true)) getNameEn() else getNameAr()
    return LookupResponse(getId(), name, whatsAppLink = getWhatsAppLink())
}

private fun KhademProfileProjection.toKhademProfileResponse(lang: String): KhademProfileResponse {
    return KhademProfileResponse(
        educationalStage = getEducationalStage().toLookupResponse(lang),
        educationalYear = getEducationalYear()?.toLookupResponse(lang),
        canApproveRequests = getCanApproveRequests(),
        responsibleStages = getResponsibleStages().map { it.toLookupResponse(lang) },
        responsibleYears = getResponsibleYears().map { it.toLookupResponse(lang) }
    )
}

private fun KahenProfileProjection.toKahenProfileResponse(lang: String): KahenProfileResponse {
    return KahenProfileResponse(
        educationalStages = getEducationalStages().map { it.toLookupResponse(lang) },
        ordinationDate = getOrdinationDate()
    )
}

private fun ParentProfileProjection.toParentProfileResponse(
    imageBaseUrl: String, 
    parentsWhatsAppLink: String?,
    children: List<UserSummaryResponse>
): ParentProfileResponse {
    return ParentProfileResponse(
        partner = getPartner()?.toUserSummaryResponse(imageBaseUrl),
        children = children,
        whatsAppLink = parentsWhatsAppLink
    )
}

private fun OrdinationProfileProjection.toOrdinationProfileResponse(lang: String, imageBaseUrl: String): OrdinationProfileResponse {
    return OrdinationProfileResponse(
        rank = getRank().toLookupResponse(lang),
        isOrdinationInAnotherChurch = getIsOrdinationInAnotherChurch(),
        ordinationYear = getOrdinationYear(),
        bishopName = getBishopName(),
        ordinationPlace = getOrdinationPlace(),
        certificateImageUrl = resolveUrl(imageBaseUrl, getCertificateImageUrl())
    )
}

private fun MakhdoomProfileProjection.toMakhdoomProfileResponse(lang: String): MakhdoomProfileResponse {
    return MakhdoomProfileResponse(
        shamamsaStudyStatus = getShamamsaStudyStatus(),
        educationalStage = getEducationalStage().toLookupResponse(lang),
        educationalYear = getEducationalYear()?.toLookupResponse(lang),
        fatherPhone = getFatherPhone(),
        fatherWhatsapp = getFatherWhatsapp(),
        motherPhone = getMotherPhone(),
        motherWhatsapp = getMotherWhatsapp(),
        isFatherDeceased = getIsFatherDeceased(),
        isMotherDeceased = getIsMotherDeceased()
    )
}