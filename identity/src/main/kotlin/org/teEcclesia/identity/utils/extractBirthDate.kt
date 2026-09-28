package org.teEcclesia.identity.utils

import java.time.LocalDate

fun extractBirthDate(nationalId: String): LocalDate {
        require(nationalId.length == 14) { "error.national_id.invalid" }

        val centuryDigit = nationalId.substring(0, 1).toInt()
        val yearPart = nationalId.substring(1, 3).toInt()
        val month = nationalId.substring(3, 5).toInt()
        val day = nationalId.substring(5, 7).toInt()

        val century = when (centuryDigit) {
            2 -> 1900
            3 -> 2000
            else -> throw IllegalArgumentException("error.national_id.invalid_century")
        }

        val year = century + yearPart
        return LocalDate.of(year, month, day)
    }

