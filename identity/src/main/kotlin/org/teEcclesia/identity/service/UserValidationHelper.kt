package org.teEcclesia.identity.service

import org.springframework.stereotype.Component
import org.teEcclesia.identity.entity.enums.UserStatus
import org.teEcclesia.identity.exception.UserAlreadyExistsException
import org.teEcclesia.identity.repository.UserRepository
import org.teEcclesia.identity.utils.formatPhone
import java.util.UUID

@Component
class UserValidationHelper(
    private val userRepository: UserRepository
) {
    fun validateEmail(email: String?, currentUserId: UUID? = null) {
        if (email.isNullOrBlank()) return
        val normalizedEmail = email.lowercase().trim()
        val exists = userRepository.existsVerifiedApprovedEmail(
            email = normalizedEmail,
            status = UserStatus.APPROVED,
            excludeUserId = currentUserId
        )
        if (exists) {
            throw UserAlreadyExistsException("Email is already registered and verified.")
        }
    }

    fun validateNationalId(nationalId: String, currentUserId: UUID? = null, isRegistration: Boolean = false) {
        val exists = userRepository.existsByNationalIdExcludingStatuses(
            nationalId = nationalId,
            excludeUserId = currentUserId
        )
        
        if (exists) {
            throw UserAlreadyExistsException("National ID is already registered.")
        }
    }

    fun validatePhone(phone: String, currentUserId: UUID? = null) {
        val formattedPhone = formatPhone(phone)

        val verifiedCount = userRepository.countVerifiedUsersByPhone(
            phone = formattedPhone,
            excludeUserId = currentUserId
        )
        if (verifiedCount >= 3) {
            throw UserAlreadyExistsException("Phone number is already registered and verified 3 times.")
        }
    }

    fun validateUserUniqueness(
        phone: String,
        nationalId: String,
        email: String?,
        currentUserId: UUID? = null,
        isRegistration: Boolean = false
    ) {
        validateNationalId(nationalId = nationalId, currentUserId = currentUserId, isRegistration = isRegistration)
        validatePhone(phone = phone, currentUserId = currentUserId)
        validateEmail(email = email, currentUserId = currentUserId)
    }
}
