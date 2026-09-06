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
import org.teEcclesia.identity.entity.enums.UserRole

import org.springframework.data.jpa.repository.EntityGraph
import org.teEcclesia.identity.repository.projection.UserProfileProjection

interface UserRepository : JpaRepository<User, UUID> {
    fun findByCode(code: String): User?
    fun existsByCodeLike(codePattern: String): Boolean
    fun findAllByCodeIn(codes: List<String>): List<User>
    fun findByNationalId(nationalId: String): User?
    fun findByNationalIdAndStatus(nationalId: String, status: UserStatus): User?
    fun findFirstByNationalIdAndStatusNotIn(nationalId: String, statuses: Collection<UserStatus>): User?
    fun findUsersByPhone(phone: String): List<User>
    fun findUsersByPhoneAndStatus(phone: String, status: UserStatus): List<User>
    fun findByEmail(email: String): User?
    fun findByEmailAndStatusAndIsEmailVerifiedIsTrue(email: String, status: UserStatus): User?
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

    @Query(
        """
            SELECT COUNT(u)
            FROM User u
            WHERE u.phone = :phone
                AND u.isPhoneVerified = true
                AND u.status NOT IN (:excludedStatuses)
                AND (:excludeUserId IS NULL OR u.id != :excludeUserId)
        """
    )
    fun countVerifiedUsersByPhone(
        @Param("phone") phone: String,
        @Param("excludedStatuses") excludedStatuses: Collection<UserStatus> = listOf(UserStatus.REJECTED, UserStatus.BANNED),
        @Param("excludeUserId") excludeUserId: UUID? = null
    ): Long

    @Query(
        """
            SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END
            FROM User u
            WHERE u.nationalId = :nationalId
                AND u.status NOT IN (:excludedStatuses)
                AND (:excludeUserId IS NULL OR u.id != :excludeUserId)
        """
    )
    fun existsByNationalIdExcludingStatuses(
        @Param("nationalId") nationalId: String,
        @Param("excludedStatuses") excludedStatuses: Collection<UserStatus> = listOf(UserStatus.REJECTED, UserStatus.BANNED),
        @Param("excludeUserId") excludeUserId: UUID? = null
    ): Boolean

    fun findFirstByPhoneAndNationalIdAndIsPhoneVerifiedFalse(phone: String, nationalId: String): User?

    @EntityGraph(value = User.GRAPH_FULL_PROFILE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    fun findProfileById(@Param("id") id: UUID): User?

    @Query("""
        SELECT u FROM User u 
        WHERE (u.email = :id OR u.nationalId = :id OR u.code = :id OR u.phone = :id)
        ORDER BY u.isPhoneVerified DESC, u.createdAt DESC
    """)
    fun findUsersByIdentifier(@Param("id") id: String): List<User>
    
    @EntityGraph(
        attributePaths = [
            "confessionPriest",
            "confessionPriest.khademProfile",
            "confessionPriest.kahenProfile",
            "confessionPriest.parentProfile",
            "confessionPriest.ordinationProfile",
            "confessionPriest.makhdoomProfile",

            "parentProfile", 
            "parentProfile.partner",
            "parentProfile.partner.khademProfile",
            "parentProfile.partner.kahenProfile",
            "parentProfile.partner.parentProfile",
            "parentProfile.partner.ordinationProfile",
            "parentProfile.partner.makhdoomProfile",

            "khademProfile", "khademProfile.educationalStage", "khademProfile.educationalYear",
            "kahenProfile",
            "ordinationProfile", "ordinationProfile.rank",
            "makhdoomProfile", "makhdoomProfile.educationalStage", "makhdoomProfile.educationalYear"
        ]
    )
    @Query("""
        SELECT u FROM User u 
        LEFT JOIN u.makhdoomProfile mp 
        LEFT JOIN u.khademProfile kp 
        WHERE u.status = :status 
        AND (cast(:stageIds as string) IS NULL OR mp.educationalStage.id IN :stageIds OR kp.educationalStage.id IN :stageIds) 
        AND (cast(:yearIds as string) IS NULL OR mp.educationalYear.id IN :yearIds OR kp.educationalYear.id IN :yearIds)
        AND (cast(:role as string) IS NULL OR u.role = :role)
        AND (cast(:search as string) IS NULL OR 
             LOWER(CONCAT(u.firstName, ' ', u.secondName, ' ', u.thirdName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', cast(:search as string), '%')) OR 
             u.phone LIKE CONCAT('%', cast(:search as string), '%') OR 
             u.nationalId LIKE CONCAT('%', cast(:search as string), '%') OR 
             LOWER(u.email) LIKE LOWER(CONCAT('%', cast(:search as string), '%')) OR 
             LOWER(u.code) LIKE LOWER(CONCAT('%', cast(:search as string), '%')))
    """)
    fun findProfilesByStatusAndFiltersProjection(
        @Param("status") status: UserStatus,
        @Param("stageIds") stageIds: Collection<Long>?,
        @Param("yearIds") yearIds: Collection<Long>?,
        @Param("role") role: UserRole?,
        @Param("search") search: String?,
        pageable: Pageable
    ): Page<UserProfileProjection>
    
    @Query("SELECT p.id as parentId, c.id as childId, c.displayName as displayName, c.code as code, c.imageUrl as imageUrl FROM ParentProfile p JOIN p.children c WHERE p.id IN :parentIds")
    fun findChildrenByParentIds(@Param("parentIds") parentIds: Collection<Long>): List<org.teEcclesia.identity.repository.projection.ParentChildProjection>

    @EntityGraph(value = User.GRAPH_WITH_PROFILES)
    fun findByRoleAndStatusIs(role: UserRole, status: UserStatus, pageable: Pageable): Page<User>

    fun countByRole(role: UserRole): Long

    @EntityGraph(value = User.GRAPH_WITH_PROFILES)
    @Query("""
        SELECT u FROM User u 
        WHERE u.role = :role AND u.status = :status
        AND ((u.email = :query AND u.isEmailVerified = true) OR u.nationalId = :query OR u.code = :query OR u.phone = :query)
    """)
    fun findByRoleAndIdentifier(
        @Param("role") role: UserRole,
        @Param("status") status: UserStatus,
        @Param("query") query: String
    ): List<User>

    @EntityGraph(value = User.GRAPH_WITH_PROFILES)
    @Query("""
        SELECT u FROM User u 
        LEFT JOIN u.khademProfile kp 
        WHERE u.status = :approvedStatus 
        AND u.deleted = false
        AND (u.role = :adminRole OR (u.role = :khademRole AND kp.canApproveRequests = true))
    """)
    fun findApprovers(
        @Param("adminRole") adminRole: UserRole = UserRole.ADMIN,
        @Param("khademRole") khademRole: UserRole = UserRole.KHADEM,
        @Param("approvedStatus") approvedStatus: UserStatus = UserStatus.APPROVED,
        pageable: Pageable
    ): Page<User>
    
    fun deleteAllByIsPhoneVerifiedIsFalseAndCreatedAtBefore(date: Instant)
}