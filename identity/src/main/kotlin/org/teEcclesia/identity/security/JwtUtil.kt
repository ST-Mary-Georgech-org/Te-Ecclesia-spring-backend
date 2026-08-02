package org.teEcclesia.identity.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.teEcclesia.client.TokenProvider
import java.time.Duration
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtUtil(
    @Value("\${jwt.secret-key}") private val secret: String
) : TokenProvider {

    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret))
    }
    private val accessExpiration = Duration.ofMinutes(30)
    private val refreshExpiration = Duration.ofDays(14)

    // ================== Generate ==================

    private fun createToken(id: UUID, expirationTimeMillis: Long, type: String): String {
        return Jwts.builder()
            .setSubject(id.toString())
            .claim("type", type)
            .setIssuedAt(Date(System.currentTimeMillis()))
            .setExpiration(Date(System.currentTimeMillis() + expirationTimeMillis))
            .signWith(secretKey)
            .compact()
    }

    override fun generateAccessToken(id: UUID): String {
        return createToken(id, accessExpiration.toMillis(), "access")
    }

    fun generateRefreshToken(id: UUID): String {
        return createToken(id, refreshExpiration.toMillis(), "refresh")
    }

    fun generateRegistrationToken(id: UUID): String {
        return createToken(id, Duration.ofHours(2).toMillis(), "registration")
    }

    // ================== Parsing ==================

    private fun parseAllClaims(token: String) =
        try {
            Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .body
        } catch (e: Exception) {
            null
        }

    fun extractUserId(token: String): UUID? {
        val subject = parseAllClaims(token)?.subject ?: return null
        return try {
            UUID.fromString(subject)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    // ================== Validation ==================

    private fun isTokenExpired(claims: Claims): Boolean {
        return claims.expiration.before(Date())
    }

    private fun validateTokenInternal(token: String): Claims? {
        val claims = parseAllClaims(token) ?: return null
        if (isTokenExpired(claims)) return null
        return claims
    }

    fun validateAccessToken(token: String): Boolean {
        val claims = validateTokenInternal(token) ?: return false
        return claims["type"] == "access"
    }

    fun validateRefreshToken(token: String): Boolean {
        val claims = validateTokenInternal(token) ?: return false
        return claims["type"] == "refresh"
    }

    fun validateRegistrationToken(token: String): Boolean {
        val claims = validateTokenInternal(token) ?: return false
        return claims["type"] == "registration"
    }

    fun validateTokenForUser(token: String, userId: UUID): Boolean {
        val claims = validateTokenInternal(token) ?: return false
        return claims.subject == userId.toString()
    }

}

