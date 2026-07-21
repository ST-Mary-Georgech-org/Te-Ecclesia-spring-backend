package org.teEcclesia.identity.api.dto.response

import org.teEcclesia.identity.entity.enums.ShamamsaStudyStatus

data class MakhdoomProfileResponse(
    val shamamsaStudyStatus: ShamamsaStudyStatus,
    val educationalStage: LookupResponse,
    val educationalYear: LookupResponse? = null,
    val fatherPhone: String? = null,
    val fatherWhatsapp: String? = null,
    val motherPhone: String? = null,
    val motherWhatsapp: String? = null,
    val isFatherDeceased: Boolean = false,
    val isMotherDeceased: Boolean = false,
    val identityDocumentImageUrl: String? = null
)
