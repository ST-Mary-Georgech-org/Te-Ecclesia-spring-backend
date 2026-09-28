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
import org.teEcclesia.identity.utils.formatHomePhone
import org.teEcclesia.identity.utils.formatPhone
import java.time.Instant
import java.util.UUID

data class RegisterRequest(
    @field:NotBlank(message = "{validation.first_name.required}")
    val firstName: String,

    @field:NotBlank(message = "{validation.second_name.required}")
    val secondName: String,

    @field:NotBlank(message = "{validation.third_name.required}")
    val thirdName: String,

    @field:NotBlank(message = "{validation.last_name.required}")
    val lastName: String,

    @field:NotBlank(message = "{validation.display_name.required}")
    val displayName: String,

    @field:NotBlank(message = "{validation.national_id.required}")
    @field:Size(min = 14, max = 14, message = "{validation.national_id.size}")
    val nationalId: String,

    @field:NotBlank(message = "{validation.phone.required}")
    val phone: String,

    val homePhone: String? = null,

    @field:Email(message = "{validation.email.invalid}")
    val email: String? = null,

    @field:Pattern(
        regexp = """^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$""",
        message = "{validation.password.pattern}"
    )
    val password: String? = null,

    val imageUrl: String? = null,
    val identityDocumentImageUrl: String? = null,

    val job: String? = null,

    @field:NotBlank(message = "{validation.building_no.required}")
    val buildingNo: String,

    @field:NotBlank(message = "{validation.street.required}")
    val street: String,

    val streetBranch: String? = null,

    @field:NotBlank(message = "{validation.area.required}")
    val area: String,

    @field:NotBlank(message = "{validation.floor.required}")
    val floor: String,

    val apartment: String? = null,

    @field:NotBlank(message = "{validation.special_mark.required}")
    val specialMark: String,

    val role: UserRole? = null,

    val confessionPriestId: UUID? = null,
    val externalConfessionPriestName: String? = null,
    val externalConfessionChurch: String? = null,
    val externalConfessionPhone: String? = null,

    val ordinationProfile: OrdinationProfileRequest? = null,
    val makhdoomProfile: MakhdoomProfileRequest? = null,
    val kahenProfile: KahenProfileRequest? = null,
    val parentProfile: ParentProfileRequest? = null,
    val khademProfile: KhademProfileRequest? = null,
    val adminKhademProfile: AdminKhademProfileRequest? = null,
    val deaconsSchoolRecord: DeaconsSchoolRecordRequest? = null
)


fun RegisterRequest.toEntity(
    hashedPassword: String, 
    confessionPriest: User? = null, 
    id: UUID = UUID.randomUUID(),
    imageUrl: String? = this.imageUrl,
    identityDocumentImageUrl: String? = this.identityDocumentImageUrl
): User {
    return User(
        id = id,
        firstName = this.firstName,
        secondName = this.secondName,
        thirdName = this.thirdName,
        lastName = this.lastName,
        displayName = this.displayName,
        nationalId = this.nationalId,
        phone = formatPhone(this.phone),
        homePhone = formatHomePhone(this.homePhone),
        email = this.email,
        passwordHash = hashedPassword,
        imageUrl = imageUrl,
        identityDocumentImageUrl = identityDocumentImageUrl,
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
        isEmailVerified = false,
        isPhoneVerified = false
    )
}
