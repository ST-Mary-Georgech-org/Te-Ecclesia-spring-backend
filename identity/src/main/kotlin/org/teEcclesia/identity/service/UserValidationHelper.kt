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
        val exists = if (currentUserId != null) {
            userRepository.existsByNationalIdAndIdNotAndStatus(nationalId, currentUserId, UserStatus.APPROVED)
        } else {
            userRepository.existsByNationalIdAndStatus(nationalId, UserStatus.APPROVED)
        }
        
        if (exists) {
            throw UserAlreadyExistsException("National ID is already registered.")
        }
    }

    fun validatePhone(phone: String, nationalId: String, currentUserId: UUID? = null) {
        val formattedPhone = formatPhone(phone)
        val existingUsersByPhone = userRepository.findUsersByPhone(formattedPhone)
        val otherVerifiedPhoneUsers = existingUsersByPhone.filter { it.isPhoneVerified && it.id != currentUserId }

        if (otherVerifiedPhoneUsers.size >= 2) {
            throw UserAlreadyExistsException("Phone number is already registered and verified twice.")
        }

        if (otherVerifiedPhoneUsers.any { it.nationalId == nationalId }) {
            throw UserAlreadyExistsException("National ID is already registered.")
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
        validatePhone(phone = phone, nationalId = nationalId, currentUserId = currentUserId)
        validateEmail(email = email, currentUserId = currentUserId)
    }
}
