package org.teEcclesia.identity.api.dto.response

import org.teEcclesia.identity.entity.User

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
)

fun User.toProfileResponse(imageBaseUrl: String): ProfileResponse {
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
    )
}