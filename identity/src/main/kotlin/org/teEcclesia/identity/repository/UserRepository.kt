package org.teEcclesia.identity.repository

import org.teEcclesia.identity.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.*

interface UserRepository : JpaRepository<User, UUID> {
    fun findByUsername(username: String): User?
    fun findByPhone(phone: String): User?
    fun findByEmail(email: String): User?
    fun deleteAllByIsPhoneVerifiedIsFalseAndCreatedAtBefore(date: Instant)
}