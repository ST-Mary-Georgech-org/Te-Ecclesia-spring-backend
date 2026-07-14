package org.teEcclesia.identity.api.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import org.teEcclesia.identity.entity.User
import java.time.Instant
import java.util.UUID

data class RegisterRequest(
    @field:NotBlank(message = "Username is required")
    val username: String,

    @field:NotBlank(message = "Full name is required")
    val fullName: String,

    @field:NotBlank(message = "Phone number is required")
    val phone: String,

    @field:Email(message = "Please provide a valid email address")
    val email: String? = null,

    @field:NotBlank(message = "Password is required")
    @field:Pattern(
        regexp = """^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$""",
        message = "Password must contain at least 8 characters, one uppercase, one lowercase, one number and one special character"
    )
    val password: String
)

fun RegisterRequest.toEntity(hashedPassword: String, id: UUID = UUID.randomUUID()): User {
    return User(
        id = id,
        username = this.username,
        fullName = this.fullName,
        phone = this.phone,
        email = this.email,
        passwordHash = hashedPassword,
        createdAt = Instant.now(),
        accountVerifications = mutableListOf(),
        refreshTokens = mutableListOf(),
        isEmailVerified = false,
        isPhoneVerified = false
    )
}
