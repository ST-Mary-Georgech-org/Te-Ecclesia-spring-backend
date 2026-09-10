package org.teEcclesia.identity.api.dto.response

import org.teEcclesia.identity.entity.DeaconsSchoolRecord
import org.teEcclesia.identity.entity.enums.DeaconsSchoolStatus
import java.math.BigDecimal

data class DeaconsSchoolRecordResponse(
    val academicYear: Int,
    val enrolled: Boolean,
    val paid: Boolean,
    val paidAmount: BigDecimal,
    val status: DeaconsSchoolStatus
)

fun DeaconsSchoolRecord.toResponse() = DeaconsSchoolRecordResponse(
    academicYear = academicYear,
    enrolled = enrolled,
    paid = paid,
    paidAmount = paidAmount,
    status = status
)

fun DeaconsSchoolRecordResponse?.orEmpty(currentYear: Int) = this ?: DeaconsSchoolRecordResponse(
    academicYear = currentYear,
    enrolled = false,
    paid = false,
    paidAmount = BigDecimal.ZERO,
    status = DeaconsSchoolStatus.PENDING
)