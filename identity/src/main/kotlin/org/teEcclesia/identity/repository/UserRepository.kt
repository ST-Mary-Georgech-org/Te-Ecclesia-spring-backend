package org.teEcclesia.identity.repository

import org.teEcclesia.identity.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.teEcclesia.identity.entity.enums.UserStatus
import org.springframework.stereotype.Repository
import org.teEcclesia.identity.entity.enums.UserRole

interface UserRepository : JpaRepository<User, UUID> {
    fun findByCode(code: String): User?
    fun findAllByCodeIn(codes: List<String>): List<User>
    fun findByNationalId(nationalId: String): User?
    fun findByPhone(phone: String): User?
    fun findByEmail(email: String): User?
    
    @Query("SELECT MAX(u.code) FROM User u WHERE u.code LIKE concat(:prefix, '%')")
    fun findMaxCodeByPrefix(prefix: String): String?
    fun findByStatus(status: UserStatus, pageable: Pageable): Page<User>
    
    fun findByRole(role: UserRole, pageable: Pageable): Page<User>
    
    fun deleteAllByIsPhoneVerifiedIsFalseAndCreatedAtBefore(date: Instant)
}