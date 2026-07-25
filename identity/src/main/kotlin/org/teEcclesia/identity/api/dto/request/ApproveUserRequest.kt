package org.teEcclesia.identity.api.dto.request

data class ApproveUserRequest(
    val customCode: String? = null,
    val updateProfileData: RegisterRequest? = null
)
