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

import org.springframework.data.jpa.repository.EntityGraph

interface UserRepository : JpaRepository<User, UUID> {
    fun findByCode(code: String): User?
    fun existsByCodeLike(codePattern: String): Boolean
    fun findAllByCodeIn(codes: List<String>): List<User>
    fun findByNationalId(nationalId: String): User?
    fun findByNationalIdAndStatus(nationalId: String, status: UserStatus): User?
    fun findByPhone(phone: String): User?
    fun findUsersByPhone(phone: String): List<User>
    fun findUsersByPhoneAndStatus(phone: String, status: UserStatus): List<User>
    fun findByEmail(email: String): User?
    fun findByEmailAndStatus(email: String, status: UserStatus): User?
    fun findUsersByEmail(email: String): List<User>
    fun findByEmailIgnoreCase(email: String): List<User>

    @Query(
        """
            SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END 
            FROM User u 
                WHERE LOWER(u.email) = LOWER(:email)
                    AND u.status = :status
                    AND u.isEmailVerified = true
                    AND (:excludeUserId IS NULL OR u.id != :excludeUserId)"""
    )
    fun existsVerifiedApprovedEmail(
        @Param("email") email: String,
        @Param("status") status: UserStatus = UserStatus.APPROVED,
        @Param("excludeUserId") excludeUserId: UUID? = null
    ): Boolean
    
    @Query("SELECT MAX(u.code) FROM User u WHERE u.code LIKE concat(:prefix, '%')")
    fun findMaxCodeByPrefix(prefix: String): String?
    fun findByStatus(status: UserStatus, pageable: Pageable): Page<User>

    @EntityGraph(
        attributePaths = [
            "confessionPriest",
            "khademProfile", "khademProfile.educationalStage", "khademProfile.educationalYear",
            "kahenProfile",
            "parentProfile", "parentProfile.partner",
            "ordinationProfile", "ordinationProfile.rank",
            "makhdoomProfile", "makhdoomProfile.educationalStage", "makhdoomProfile.educationalYear"
        ]
    )
    @Query("SELECT u FROM User u WHERE u.id = :id")
    fun findProfileById(@Param("id") id: UUID): User?
    
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
    
    @EntityGraph(
        attributePaths = [
            "confessionPriest",
            "khademProfile", "khademProfile.educationalStage", "khademProfile.educationalYear",
            "kahenProfile",
            "parentProfile", "parentProfile.partner",
            "ordinationProfile", "ordinationProfile.rank",
            "makhdoomProfile", "makhdoomProfile.educationalStage", "makhdoomProfile.educationalYear"
        ]
    )
    @Query("""
        SELECT u FROM User u 
        LEFT JOIN u.makhdoomProfile mp 
        LEFT JOIN u.khademProfile kp 
        WHERE u.status = :status 
        AND (:stageId IS NULL OR mp.educationalStage.id = :stageId OR kp.educationalStage.id = :stageId) 
        AND (:yearId IS NULL OR mp.educationalYear.id = :yearId OR kp.educationalYear.id = :yearId)
        AND (cast(:role as string) IS NULL OR u.role = :role)
        AND (cast(:search as string) IS NULL OR 
             LOWER(CONCAT(u.firstName, ' ', u.secondName, ' ', u.thirdName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', cast(:search as string), '%')) OR 
             u.phone LIKE CONCAT('%', cast(:search as string), '%') OR 
             u.nationalId LIKE CONCAT('%', cast(:search as string), '%') OR 
             LOWER(u.email) LIKE LOWER(CONCAT('%', cast(:search as string), '%')) OR 
             LOWER(u.code) LIKE LOWER(CONCAT('%', cast(:search as string), '%')))
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

    fun findByRoleAndStatusIs(role: UserRole, status: UserStatus, pageable: Pageable): Page<User>

    fun countByRole(role: UserRole): Long

    @Query("""
        SELECT u FROM User u 
        WHERE u.role = :role AND u.status = :status
        AND (u.email = :query OR u.nationalId = :query OR u.code = :query OR u.phone = :query)
    """)
    fun findByRoleAndIdentifier(
        @Param("role") role: UserRole,
        @Param("status") status: UserStatus,
        @Param("query") query: String
    ): List<User>
    
    @Query("SELECT u FROM User u JOIN u.khademProfile kp WHERE u.role = :role AND kp.canApproveRequests = true")
    fun findByRoleAndCanApproveRequestsTrue(@Param("role") role: UserRole, pageable: Pageable): Page<User>

    @Query("""
        SELECT u FROM User u 
        LEFT JOIN u.khademProfile kp 
        WHERE u.role = :adminRole OR (u.role = :khademRole AND kp.canApproveRequests = true)
    """)
    fun findApprovers(
        @Param("adminRole") adminRole: UserRole = UserRole.ADMIN,
        @Param("khademRole") khademRole: UserRole = UserRole.KHADEM,
        pageable: Pageable
    ): Page<User>
    
    fun deleteAllByIsPhoneVerifiedIsFalseAndCreatedAtBefore(date: Instant)
}