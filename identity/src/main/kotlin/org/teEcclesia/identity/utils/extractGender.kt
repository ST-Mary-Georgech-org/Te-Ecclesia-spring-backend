package org.teEcclesia.identity.utils

import org.teEcclesia.identity.entity.enums.Gender

fun extractGender(nationalId: String): Gender {
    require(nationalId.length == 14) { "error.national_id.invalid" }

    val genderDigit = nationalId.substring(12, 13).toInt()
    return if (genderDigit % 2 != 0) Gender.MALE else Gender.FEMALE
}
