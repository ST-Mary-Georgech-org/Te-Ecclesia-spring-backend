package org.teEcclesia.identity.api.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.teEcclesia.identity.entity.User
import org.teEcclesia.identity.entity.enums.Gender
import org.teEcclesia.identity.entity.enums.UserRole
import org.teEcclesia.identity.entity.enums.UserStatus
import java.time.Instant
import java.time.LocalDate
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

    val birthDate: LocalDate,

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

    @field:NotBlank(message = "Apartment is required")
    val apartment: String,

    val specialMark: String? = null,

    val gender: Gender,

    val role: UserRole? = null,

    val confessionPriestId: UUID? = null,
    val externalConfessionPriestName: String? = null,
    val externalConfessionChurch: String? = null,

    val ordinationProfile: OrdinationProfileRequest? = null,
    val makhdoomProfile: MakhdoomProfileRequest? = null
)

fun RegisterRequest.toEntity(hashedPassword: String, confessionPriest: User? = null, id: UUID = UUID.randomUUID()): User {
    return User(
        id = id,
        firstName = this.firstName,
        secondName = this.secondName,
        thirdName = this.thirdName,
        lastName = this.lastName,
        displayName = this.displayName,
        nationalId = this.nationalId,
        phone = this.phone,
        homePhone = this.homePhone,
        email = this.email,
        passwordHash = hashedPassword,
        imageUrl = this.imageUrl,
        createdAt = Instant.now(),
        birthDate = this.birthDate,
        job = this.job,
        buildingNo = this.buildingNo,
        street = this.street,
        streetBranch = this.streetBranch,
        area = this.area,
        floor = this.floor,
        apartment = this.apartment,
        specialMark = this.specialMark,
        gender = this.gender,
        status = UserStatus.PROFILE_INCOMPLETE,
        role = this.role ?: UserRole.GUEST,
        confessionPriest = confessionPriest,
        externalConfessionPriestName = this.externalConfessionPriestName,
        externalConfessionChurch = this.externalConfessionChurch,
        accountVerifications = mutableListOf(),
        refreshTokens = mutableListOf(),
        isEmailVerified = false,
        isPhoneVerified = false
    )
}
