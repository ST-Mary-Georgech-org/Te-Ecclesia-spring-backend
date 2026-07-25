package org.teEcclesia.identity.api.dto.response

import org.springframework.context.i18n.LocaleContextHolder
import org.teEcclesia.identity.entity.User
import java.time.Instant
import org.teEcclesia.identity.entity.enums.Gender
import org.teEcclesia.identity.entity.enums.UserStatus
import org.teEcclesia.identity.entity.enums.UserRole

data class ProfileResponse(
    val id: String,
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
    val actionTakenAt: Instant? = null
)

fun User.toUserSummaryResponse(imageBaseUrl: String): UserSummaryResponse {
    val resolvedImageUrl = if (imageUrl.isNullOrBlank()) null else "$imageBaseUrl/$imageUrl"
    return UserSummaryResponse(
        id = id,
        name = displayName,
        code = code,
        imageUrl = resolvedImageUrl
    )
}

fun User.toProfileResponse(imageBaseUrl: String): ProfileResponse {
    val lang = LocaleContextHolder.getLocale().language
    val resolvedImageUrl = if (imageUrl.isNullOrBlank()) {
        null
    } else {
        "$imageBaseUrl/$imageUrl"
    }
    return ProfileResponse(
        id = id.toString(),
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
        imageUrl = resolvedImageUrl,
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
                educationalStage = LookupResponse(it.educationalStage.id, stageName),
                educationalYear = it.educationalYear?.let { year -> 
                    val yearName = if (lang.startsWith("en", ignoreCase = true)) year.nameEn else year.nameAr
                    LookupResponse(year.id, yearName)
                }
            )
        },
        kahenProfile = this.kahenProfile?.let {
            KahenProfileResponse(
                educationalStages = it.educationalStages.map { stage ->
                    val stageName = if (lang.startsWith("en", ignoreCase = true)) stage.nameEn else stage.nameAr
                    LookupResponse(stage.id, stageName)
                },
                ordinationDate = it.ordinationDate
            )
        },
        parentProfile = this.parentProfile?.let {
            ParentProfileResponse(
                partner = it.partner?.toUserSummaryResponse(imageBaseUrl),
                children = it.children.map { child -> child.toUserSummaryResponse(imageBaseUrl) }
            )
        },
        ordinationProfile = this.ordinationProfile?.let {
            val rankName = if (lang.startsWith("en", ignoreCase = true)) it.rank.nameEn else it.rank.nameAr
            OrdinationProfileResponse(
                rank = LookupResponse(it.rank.id, rankName),
                isOrdinationInAnotherChurch = it.isOrdinationInAnotherChurch,
                ordinationYear = it.ordinationYear,
                bishopName = it.bishopName,
                ordinationPlace = it.ordinationPlace,
                certificateImageUrl = if (it.certificateImageUrl.isNullOrBlank()) null else "$imageBaseUrl/${it.certificateImageUrl}"
            )
        },
        makhdoomProfile = this.makhdoomProfile?.let {
            val stageName = if (lang.startsWith("en", ignoreCase = true)) it.educationalStage.nameEn else it.educationalStage.nameAr
            MakhdoomProfileResponse(
                shamamsaStudyStatus = it.shamamsaStudyStatus,
                educationalStage = LookupResponse(it.educationalStage.id, stageName),
                educationalYear = it.educationalYear?.let { year ->
                    val yearName = if (lang.startsWith("en", ignoreCase = true)) year.nameEn else year.nameAr
                    LookupResponse(year.id, yearName)
                },
                fatherPhone = it.fatherPhone,
                fatherWhatsapp = it.fatherWhatsapp,
                motherPhone = it.motherPhone,
                motherWhatsapp = it.motherWhatsapp,
                isFatherDeceased = it.isFatherDeceased,
                isMotherDeceased = it.isMotherDeceased,
                identityDocumentImageUrl = if (it.identityDocumentImageUrl.isNullOrBlank()) null else "$imageBaseUrl/${it.identityDocumentImageUrl}"
            )
        },
        createdAt = this.createdAt,
        actionTakenAt = this.actionTakenAt
    )
}