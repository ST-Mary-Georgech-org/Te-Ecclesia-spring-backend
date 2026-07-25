package org.teEcclesia.identity.utils

import java.time.LocalDate

fun extractBirthDate(nationalId: String): LocalDate {
        require(nationalId.length == 14) { "National ID must be exactly 14 digits" }

        val centuryDigit = nationalId.substring(0, 1).toInt()
        val yearPart = nationalId.substring(1, 3).toInt()
        val month = nationalId.substring(3, 5).toInt()
        val day = nationalId.substring(5, 7).toInt()

        val century = when (centuryDigit) {
            2 -> 1900
            3 -> 2000
            else -> throw IllegalArgumentException("Unsupported or invalid birth century digit: $centuryDigit")
        }

        val year = century + yearPart
        return LocalDate.of(year, month, day)
    }

