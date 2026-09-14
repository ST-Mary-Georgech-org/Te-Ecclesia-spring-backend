package org.teEcclesia.identity.repository

import org.teEcclesia.identity.entity.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.*

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByToken(token: String): RefreshToken?
    fun findByUserIdAndToken(userId: UUID, token: String): RefreshToken?
    fun findAllByUserId(userId: UUID): List<RefreshToken>
    fun findAllByDeviceToken(deviceToken: String): List<RefreshToken>
    fun deleteAllByExpiryDateBefore(date: Instant)

    @Query("SELECT DISTINCT r.deviceToken FROM RefreshToken r WHERE r.user.id = :userId AND r.deviceToken IS NOT NULL AND TRIM(r.deviceToken) != ''")
    fun findDeviceTokensByUserId(@Param("userId") userId: UUID): List<String>

    @Modifying
    @Query("UPDATE RefreshToken r SET r.deviceToken = null WHERE r.deviceToken = :deviceToken")
    fun clearDeviceToken(deviceToken: String): Int
}