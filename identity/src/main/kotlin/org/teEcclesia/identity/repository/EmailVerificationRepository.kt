package org.teEcclesia.identity.repository

import org.teEcclesia.identity.entity.AccountVerification
import org.teEcclesia.identity.entity.User
import org.teEcclesia.identity.entity.VerificationMethod
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.*

interface EmailVerificationRepository : JpaRepository<AccountVerification, UUID> {
    fun findByOtpAndUser(otp: String, user: User): AccountVerification?
    fun findTopByOtpAndUserAndMethod(otp: String, user: User, method: VerificationMethod): AccountVerification?
    fun findByOtpAndPhone(otp: String, phone: String): AccountVerification?
    fun findByOtpAndEmail(otp: String, email: String): AccountVerification?
    fun findByOtpAndMethod(otp: String, method: VerificationMethod): AccountVerification?
    fun findByOtpInAndMethod(otps: List<String>, method: VerificationMethod): AccountVerification?
    fun deleteAllByUserAndMethod(user: User, method: VerificationMethod)
    fun deleteAllBySentAtBefore(date: Instant)
}