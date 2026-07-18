package org.teEcclesia.identity.api.dto.request

import org.teEcclesia.identity.entity.enums.UserRole

data class CompleteProfileRequest(
    val role: UserRole,

    val ordinationProfile: OrdinationProfileRequest? = null,
    val makhdoomProfile: MakhdoomProfileRequest? = null,
    val khademProfile: KhademProfileRequest? = null,
    val parentProfile: ParentProfileRequest? = null
)
