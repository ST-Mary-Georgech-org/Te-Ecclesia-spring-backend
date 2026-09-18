package org.teEcclesia.identity.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import org.teEcclesia.identity.entity.AccountDeletionRequest
import org.teEcclesia.identity.repository.projection.AccountDeletionRequestProjection
import java.util.UUID

interface AccountDeletionRequestRepository : JpaRepository<AccountDeletionRequest, UUID> {

    @Query(
        nativeQuery = true,
        value = """
            SELECT 
                r.id as id,
                u.id as userId,
                CONCAT(u.first_name, ' ', u.second_name, ' ', u.third_name, ' ', u.last_name) as userName,
                u.code as userCode,
                u.image_url as userImageUrl,
                u.role as userRole,
                r.reason as reason,
                r.requested_at as requestedAt
            FROM identity.account_deletion_requests r
            JOIN identity.users u ON r.user_id = u.id
            WHERE r.user_id = :userId
            LIMIT 1
        """
    )
    fun findProjectionByUserId(@Param("userId") userId: UUID): AccountDeletionRequestProjection?

    @Query(
        nativeQuery = true,
        value = """
            SELECT 
                r.id as id,
                u.id as userId,
                CONCAT(u.first_name, ' ', u.second_name, ' ', u.third_name, ' ', u.last_name) as userName,
                u.code as userCode,
                u.image_url as userImageUrl,
                u.role as userRole,
                r.reason as reason,
                r.requested_at as requestedAt
            FROM identity.account_deletion_requests r
            JOIN identity.users u ON r.user_id = u.id
            WHERE r.id = :requestId
            LIMIT 1
        """
    )
    fun findProjectionById(@Param("requestId") requestId: UUID): AccountDeletionRequestProjection?

    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = "DELETE FROM identity.account_deletion_requests WHERE id = :id")
    fun deleteByRequestId(@Param("id") id: UUID): Int

    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = "DELETE FROM identity.account_deletion_requests WHERE user_id = :userId")
    fun deleteByUserId(@Param("userId") userId: UUID): Int

    @Query(nativeQuery = true, value = "SELECT COUNT(*) FROM identity.account_deletion_requests")
    fun countAll(): Long

    @Query(
        nativeQuery = true,
        value = """
            SELECT 
                r.id as id,
                u.id as userId,
                CONCAT(u.first_name, ' ', u.second_name, ' ', u.third_name, ' ', u.last_name) as userName,
                u.code as userCode,
                u.image_url as userImageUrl,
                u.role as userRole,
                r.reason as reason,
                r.requested_at as requestedAt
            FROM identity.account_deletion_requests r
            JOIN identity.users u ON r.user_id = u.id
            ORDER BY r.requested_at DESC
        """,
        countQuery = "SELECT COUNT(*) FROM identity.account_deletion_requests"
    )
    fun findAllPaged(pageable: Pageable): Page<AccountDeletionRequestProjection>
}
