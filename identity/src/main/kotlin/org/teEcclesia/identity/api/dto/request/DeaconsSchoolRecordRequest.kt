package org.teEcclesia.identity.api.dto.request

import org.teEcclesia.identity.entity.enums.DeaconsSchoolStatus
import java.math.BigDecimal

data class DeaconsSchoolRecordRequest(
    val enrolled: Boolean,
    val paid: Boolean,
    val paidAmount: BigDecimal,
    val status: DeaconsSchoolStatus
)
