package org.teEcclesia.identity.repository

import org.teEcclesia.identity.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.query.Param
import org.teEcclesia.identity.entity.enums.UserStatus
import org.springframework.stereotype.Repository
import org.teEcclesia.identity.entity.enums.UserRole

interface UserRepository : JpaRepository<User, UUID> {
    fun findByCode(code: String): User?
    fun existsByCodeLike(codePattern: String): Boolean
    fun findAllByCodeIn(codes: List<String>): List<User>
    fun findByNationalId(nationalId: String): User?
    fun findByPhone(phone: String): User?
    fun findUsersByPhone(phone: String): List<User>
    fun findByEmail(email: String): User?
    
    @Query("SELECT MAX(u.code) FROM User u WHERE u.code LIKE concat(:prefix, '%')")
    fun findMaxCodeByPrefix(prefix: String): String?
    fun findByStatus(status: UserStatus, pageable: Pageable): Page<User>
    
    @Query("""
        SELECT u FROM User u 
        WHERE (u.email = :id OR u.nationalId = :id OR u.code = :id OR u.phone = :id)
        ORDER BY u.isPhoneVerified DESC, u.createdAt DESC 
        LIMIT 1
    """)
    fun findTopByIdentifierOrderByVerification(@Param("id") id: String): User?

    @Query("""
        SELECT u FROM User u 
        WHERE (u.email = :id OR u.nationalId = :id OR u.code = :id OR u.phone = :id)
        ORDER BY u.isPhoneVerified DESC, u.createdAt DESC
    """)
    fun findUsersByIdentifier(@Param("id") id: String): List<User>
    
    @Query("""
        SELECT u FROM User u 
        LEFT JOIN u.makhdoomProfile mp 
        LEFT JOIN u.khademProfile kp 
        WHERE u.status = :status 
        AND (:stageId IS NULL OR mp.educationalStage.id = :stageId OR kp.educationalStage.id = :stageId) 
        AND (:yearId IS NULL OR mp.educationalYear.id = :yearId OR kp.educationalYear.id = :yearId)
        AND (cast(:role as string) IS NULL OR u.role = :role)
        AND (:search IS NULL OR 
             LOWER(CONCAT(u.firstName, ' ', u.secondName, ' ', u.thirdName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :search, '%')) OR 
             u.phone LIKE CONCAT('%', :search, '%') OR 
             u.nationalId LIKE CONCAT('%', :search, '%') OR 
             LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR 
             LOWER(u.code) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    fun findByStatusAndFilters(
        @Param("status") status: UserStatus,
        @Param("stageId") stageId: Long?,
        @Param("yearId") yearId: Long?,
        @Param("role") role: UserRole?,
        @Param("search") search: String?,
        pageable: Pageable
    ): Page<User>
    
    fun findByRole(role: UserRole, pageable: Pageable): Page<User>
    fun countByRole(role: UserRole): Long

    @Query("""
        SELECT u FROM User u 
        WHERE u.role = :role AND u.isPhoneVerified
        AND (u.email = :query OR u.nationalId = :query OR u.code = :query OR u.phone = :query)
    """)
    fun findByRoleAndIdentifier(@Param("role") role: UserRole, @Param("query") query: String): List<User>
    
    @Query("SELECT u FROM User u JOIN u.khademProfile kp WHERE u.role = :role AND kp.canApproveRequests = true")
    fun findByRoleAndCanApproveRequestsTrue(@Param("role") role: UserRole, pageable: Pageable): Page<User>
    
    fun deleteAllByIsPhoneVerifiedIsFalseAndCreatedAtBefore(date: Instant)
}