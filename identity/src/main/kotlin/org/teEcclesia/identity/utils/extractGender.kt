package org.teEcclesia.identity.utils

import org.teEcclesia.identity.entity.enums.Gender

fun extractGender(nationalId: String): Gender {
        require(nationalId.length == 14) { "National ID must be exactly 14 digits" }
        
        val genderDigit = nationalId.substring(12, 13).toInt()
        return if (genderDigit % 2 != 0) Gender.MALE else Gender.FEMALE
    }
