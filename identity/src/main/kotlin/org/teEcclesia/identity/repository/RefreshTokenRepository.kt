package org.teEcclesia.identity.repository

import org.teEcclesia.identity.entity.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.*

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByToken(token: String): RefreshToken?
    fun findByUserIdAndToken(userId: UUID, token: String): RefreshToken?
    fun findAllByUserId(userId: UUID): List<RefreshToken>
    fun findAllByDeviceToken(deviceToken: String): List<RefreshToken>
    fun deleteAllByExpiryDateBefore(date: Instant)
}