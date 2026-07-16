package org.teEcclesia.identity.api.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.teEcclesia.identity.entity.User
import org.teEcclesia.identity.entity.enums.UserRole
import org.teEcclesia.identity.entity.enums.UserStatus
import org.teEcclesia.identity.utils.extractBirthDate
import org.teEcclesia.identity.utils.extractGender
import java.time.Instant
import java.util.UUID

data class RegisterRequest(
    @field:NotBlank(message = "First name is required")
    val firstName: String,

    @field:NotBlank(message = "Second name is required")
    val secondName: String,

    @field:NotBlank(message = "Third name is required")
    val thirdName: String,

    @field:NotBlank(message = "Last name is required")
    val lastName: String,

    @field:NotBlank(message = "Display name is required")
    val displayName: String,

    @field:NotBlank(message = "National ID is required")
    @field:Size(min = 14, max = 14, message = "National ID must be exactly 14 characters")
    val nationalId: String,

    @field:NotBlank(message = "Phone number is required")
    val phone: String,

    @field:NotBlank(message = "Home phone is required")
    val homePhone: String,

    @field:Email(message = "Please provide a valid email address")
    val email: String? = null,

    @field:NotBlank(message = "Password is required")
    @field:Pattern(
        regexp = """^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$""",
        message = "Password must contain at least 8 characters, one uppercase, one lowercase, one number and one special character"
    )
    val password: String,

    val imageUrl: String? = null,

    @field:NotBlank(message = "Job is required")
    val job: String,

    @field:NotBlank(message = "Building number is required")
    val buildingNo: String,

    @field:NotBlank(message = "Street is required")
    val street: String,

    val streetBranch: String? = null,

    @field:NotBlank(message = "Area is required")
    val area: String,

    @field:NotBlank(message = "Floor is required")
    val floor: String,

    val apartment: String? = null,

    @field:NotBlank(message = "Special mark is required")
    val specialMark: String,

    val role: UserRole? = null,

    val confessionPriestId: UUID? = null,
    val externalConfessionPriestName: String? = null,
    val externalConfessionChurch: String? = null,
    val externalConfessionPhone: String? = null,

    val ordinationProfile: OrdinationProfileRequest? = null,
    val makhdoomProfile: MakhdoomProfileRequest? = null,
    val parentProfile: ParentProfileRequest? = null
)

fun formatHomePhone(homePhone: String): String {
    val cleanPhone = homePhone.trim()
    return when {
        cleanPhone.length == 8 -> "02$cleanPhone"
        cleanPhone.length == 10 && cleanPhone.startsWith("02") -> cleanPhone
        else -> throw IllegalArgumentException("Invalid home phone format. Must be 8 digits, or 10 digits starting with 02.")
    }
}

fun RegisterRequest.toEntity(
    hashedPassword: String, 
    confessionPriest: User? = null, 
    id: UUID = UUID.randomUUID(),
    imageUrl: String? = this.imageUrl
): User {
    return User(
        id = id,
        firstName = this.firstName,
        secondName = this.secondName,
        thirdName = this.thirdName,
        lastName = this.lastName,
        displayName = this.displayName,
        nationalId = this.nationalId,
        phone = this.phone,
        homePhone = formatHomePhone(this.homePhone),
        email = this.email,
        passwordHash = hashedPassword,
        imageUrl = imageUrl,
        createdAt = Instant.now(),
        birthDate = extractBirthDate(this.nationalId),
        job = this.job,
        buildingNo = this.buildingNo,
        street = this.street,
        streetBranch = this.streetBranch,
        area = this.area,
        floor = this.floor,
        apartment = this.apartment,
        specialMark = this.specialMark,
        gender = extractGender(this.nationalId),
        status = UserStatus.PROFILE_INCOMPLETE,
        role = this.role ?: UserRole.GUEST,
        confessionPriest = confessionPriest,
        externalConfessionPriestName = this.externalConfessionPriestName,
        externalConfessionChurch = this.externalConfessionChurch,
        externalConfessionPhone = this.externalConfessionPhone,
        accountVerifications = mutableListOf(),
        refreshTokens = mutableListOf(),
        isEmailVerified = false,
        isPhoneVerified = false
    )
}
