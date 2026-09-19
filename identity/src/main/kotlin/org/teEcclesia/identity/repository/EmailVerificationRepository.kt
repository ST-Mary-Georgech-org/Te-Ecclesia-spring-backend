package org.teEcclesia.identity.repository

import org.teEcclesia.identity.entity.AccountVerification
import org.teEcclesia.identity.entity.User
import org.teEcclesia.identity.entity.VerificationMethod
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.*

interface EmailVerificationRepository : JpaRepository<AccountVerification, UUID> {
    fun findByOtpAndUserId(otp: String, userId: UUID): AccountVerification?
    fun findTopByOtpAndUserIdAndMethod(otp: String, userId: UUID, method: VerificationMethod): AccountVerification?
    fun findByOtpAndPhone(otp: String, phone: String): AccountVerification?
    fun findByOtpAndEmail(otp: String, email: String): AccountVerification?
    fun findByOtpAndMethod(otp: String, method: VerificationMethod): AccountVerification?
    fun findByOtpInAndMethod(otps: List<String>, method: VerificationMethod): AccountVerification?

    @Modifying
    @Query("DELETE FROM AccountVerification av WHERE av.userId = :userId AND av.method = :method")
    fun deleteAllByUserIdAndMethod(@Param("userId") userId: UUID, @Param("method") method: VerificationMethod): Int

    @Modifying
    @Query("DELETE FROM AccountVerification av WHERE av.sentAt < :date")
    fun deleteAllBySentAtBefore(@Param("date") date: Instant): Int
}