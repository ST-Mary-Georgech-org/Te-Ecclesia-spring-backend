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
        khademProfile = this.khademProfile?.let {
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
        },
        kahenProfile = this.kahenProfile?.let {
            KahenProfileResponse(
                educationalStages = it.educationalStages.map { stage ->
                    val stageName = if (lang.startsWith("en", ignoreCase = true)) stage.nameEn else stage.nameAr
                    LookupResponse(stage.id, stageName, whatsAppLink = null)
                },
                ordinationDate = it.ordinationDate
            )
        },
        parentProfile = this.parentProfile?.let {
            ParentProfileResponse(
                partner = it.partner?.toUserSummaryResponse(imageBaseUrl),
                children = it.children.map { child -> child.toUserSummaryResponse(imageBaseUrl) },
                nationalIdImageUrl = resolveUrl(imageBaseUrl, it.nationalIdImageUrl),
                whatsAppLink = parentsWhatsAppLink
            )
        },
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
        makhdoomProfile = this.makhdoomProfile?.let {
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
                isMotherDeceased = it.isMotherDeceased,
                identityDocumentImageUrl = resolveUrl(imageBaseUrl, it.identityDocumentImageUrl)
            )
        },
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
        confessionPriest = getConfessionPriest()?.toUserSummaryResponse(imageBaseUrl),
        externalConfessionPriestName = getExternalConfessionPriestName(),
        externalConfessionChurch = getExternalConfessionChurch(),
        externalConfessionPhone = getExternalConfessionPhone(),
        khademProfile = getKhademProfile()?.toKhademProfileResponse(lang),
        kahenProfile = getKahenProfile()?.toKahenProfileResponse(lang),
        parentProfile = getParentProfile()?.toParentProfileResponse(
            imageBaseUrl, 
            parentsWhatsAppLink,
            children = getParentProfile()?.getId()?.let { childrenByParentId[it] } ?: emptyList()
        ),
        ordinationProfile = getOrdinationProfile()?.toOrdinationProfileResponse(lang, imageBaseUrl),
        makhdoomProfile = getMakhdoomProfile()?.toMakhdoomProfileResponse(lang, imageBaseUrl),
        createdAt = getCreatedAt(),
        actionTakenAt = getActionTakenAt(),
        actionTakenBy = getActionTakenBy()?.toUserSummaryResponse(imageBaseUrl),
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
        nationalIdImageUrl = resolveUrl(imageBaseUrl, getNationalIdImageUrl()),
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

private fun MakhdoomProfileProjection.toMakhdoomProfileResponse(lang: String, imageBaseUrl: String): MakhdoomProfileResponse {
    return MakhdoomProfileResponse(
        shamamsaStudyStatus = getShamamsaStudyStatus(),
        educationalStage = getEducationalStage().toLookupResponse(lang),
        educationalYear = getEducationalYear()?.toLookupResponse(lang),
        fatherPhone = getFatherPhone(),
        fatherWhatsapp = getFatherWhatsapp(),
        motherPhone = getMotherPhone(),
        motherWhatsapp = getMotherWhatsapp(),
        isFatherDeceased = getIsFatherDeceased(),
        isMotherDeceased = getIsMotherDeceased(),
        identityDocumentImageUrl = resolveUrl(imageBaseUrl, getIdentityDocumentImageUrl())
    )
}