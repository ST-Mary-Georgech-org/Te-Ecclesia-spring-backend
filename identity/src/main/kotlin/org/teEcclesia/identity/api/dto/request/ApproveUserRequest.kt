package org.teEcclesia.identity.api.dto.request

data class ApproveUserRequest(
    val customCode: String? = null,
    val updateProfileData: RegisterRequest? = null,
    val deaconsSchoolRecord: DeaconsSchoolRecordRequest? = null,
    val deleteImage: Boolean? = false,
    val deleteIdentityDocument: Boolean? = false,
    val deleteOrdinationCertificate: Boolean? = false
)
