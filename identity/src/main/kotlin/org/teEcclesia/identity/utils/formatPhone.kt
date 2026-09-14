package org.teEcclesia.identity.utils

fun formatPhone(phone: String): String {
    var clean = phone.trim().replace(" ", "").replace("-", "")
    
    if (clean.startsWith("00")) {
        clean = "+" + clean.substring(2)
    }
    
    if (clean.startsWith("+02")) {
        clean = "+2" + clean.substring(3)
    }
    
    if (clean.startsWith("+200")) {
        clean = "+20" + clean.substring(4)
    }

    if (clean.startsWith("200")) {
        clean = "+20" + clean.substring(3)
    }

    if (clean.startsWith("20") && !clean.startsWith("+")) {
        clean = "+" + clean
    }
    
    val regex11 = Regex("""^01[0125]\d{8}$""")
    if (regex11.matches(clean)) {
        return "+2$clean"
    }
    
    val regexEgyptFull = Regex("""^\+201[0125]\d{8}$""")
    if (regexEgyptFull.matches(clean)) {
        return clean
    }
    
    val regexInternational = Regex("""^\+[1-9]\d{6,14}$""")
    if (regexInternational.matches(clean)) {
        return clean
    }
    
    throw IllegalArgumentException("Invalid phone number format.")
}
