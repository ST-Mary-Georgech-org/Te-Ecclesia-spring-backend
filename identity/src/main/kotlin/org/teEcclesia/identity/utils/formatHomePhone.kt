package org.teEcclesia.identity.utils

fun formatHomePhone(homePhone: String?): String? {
    if (homePhone.isNullOrBlank()) return null
    val cleanPhone = homePhone.trim()
    return when (cleanPhone.length) {
        8 -> "02$cleanPhone"
        10 if cleanPhone.startsWith("02") -> cleanPhone
        else -> throw IllegalArgumentException("error.home_phone.invalid_format")
    }
}
