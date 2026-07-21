package org.teEcclesia.identity.api.dto.response

import org.springframework.context.i18n.LocaleContextHolder
import org.teEcclesia.identity.entity.User
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
    val phone: String,
    val email: String?,
    val isEmailVerified: Boolean,
    val isPhoneVerified: Boolean,
    val imageUrl: String?,
    val specialMark: String,
    val gender: Gender,
    val status: UserStatus,
    val statusReason: String?,
    val role: UserRole,
    val khademProfile: KhademProfileResponse? = null,
    val parentProfile: ParentProfileResponse? = null
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
        phone = phone,
        email = email,
        isEmailVerified = isEmailVerified,
        isPhoneVerified = isPhoneVerified,
        imageUrl = resolvedImageUrl,
        specialMark = this.specialMark,
        gender = this.gender,
        status = this.status,
        statusReason = this.statusReason,
        role = this.role,
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
        parentProfile = this.parentProfile?.let {
            ParentProfileResponse(
                partner = it.partner?.toUserSummaryResponse(imageBaseUrl),
                children = it.children.map { child -> child.toUserSummaryResponse(imageBaseUrl) }
            )
        }
    )
}