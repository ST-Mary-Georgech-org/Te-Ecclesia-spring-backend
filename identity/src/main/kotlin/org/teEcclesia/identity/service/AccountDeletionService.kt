package org.teEcclesia.identity.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.teEcclesia.events.notifications.NotificationDetails
import org.teEcclesia.events.notifications.UserNotificationsEvent
import org.teEcclesia.events.notifications.utils.NotificationAction
import org.teEcclesia.events.notifications.utils.NotificationMedium
import org.teEcclesia.events.notifications.utils.NotificationType
import org.teEcclesia.events.publisher.TeEcclesiaEventPublisher
import org.teEcclesia.identity.api.dto.request.DeleteAccountRequest
import org.teEcclesia.identity.api.dto.request.ReactivateAccountRequest
import org.teEcclesia.identity.api.dto.response.AccountDeletionRequestResponse
import org.teEcclesia.identity.api.dto.response.AuthResponse
import org.teEcclesia.identity.entity.AccountDeletionRequest
import org.teEcclesia.identity.entity.RefreshToken
import org.teEcclesia.identity.entity.enums.UserRole
import org.teEcclesia.identity.exception.IncorrectPasswordException
import org.teEcclesia.identity.exception.InvalidCredentialsException
import org.teEcclesia.identity.exception.UnauthorizedException
import org.teEcclesia.identity.repository.AccountDeletionRequestRepository
import org.teEcclesia.identity.repository.RefreshTokenRepository
import org.teEcclesia.identity.repository.UserRepository
import org.teEcclesia.identity.security.JwtUtil
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class AccountDeletionService(
    private val userRepository: UserRepository,
    private val accountDeletionRequestRepository: AccountDeletionRequestRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtUtil: JwtUtil,
    private val eventPublisher: TeEcclesiaEventPublisher
) {
    @Transactional
    fun requestAccountDeletion(userId: UUID, request: DeleteAccountRequest) {
        val userAuth = userRepository.findAuthDetailsById(userId)
            ?: throw EntityNotFoundException("User not found")

        if (!passwordEncoder.matches(request.password, userAuth.getPasswordHash())) {
            throw IncorrectPasswordException("Invalid password")
        }

        accountDeletionRequestRepository.deleteByUserId(userId)

        val deletionRequest = AccountDeletionRequest(
            userId = userId,
            reason = request.reason.trim(),
            requestedAt = Instant.now()
        )
        accountDeletionRequestRepository.save(deletionRequest)

        userRepository.softDeleteById(userId)

        refreshTokenRepository.deleteAllByUserId(userId)

        val adminIds = userRepository.findAllAdminIds()
        if (adminIds.isNotEmpty()) {
            val notifications = adminIds.map { adminId ->
                NotificationDetails(
                    userId = adminId,
                    subject = "طلب حذف حساب",
                    message = "قام ${userAuth.getFullName()} بتقديم طلب حذف لحسابه.",
                    type = NotificationType.ALERT,
                    medium = NotificationMedium.PUSH,
                    dataPayload = mapOf("action" to NotificationAction.ACCOUNT_DELETION_REQUESTED.name, "targetUserId" to userId.toString())
                )
            }
            eventPublisher.publish(UserNotificationsEvent(notifications))
        }
    }

    @Transactional
    fun reactivateAccount(request: ReactivateAccountRequest): AuthResponse {
        val nationalId = request.nationalId.trim()
        val deletedUser = userRepository.findDeletedByNationalId(nationalId)
            ?: throw EntityNotFoundException("No deleted account found for the provided national ID")

        if (!passwordEncoder.matches(request.password, deletedUser.getPasswordHash())) {
            throw InvalidCredentialsException("Invalid password")
        }

        val userId = deletedUser.getId()
        userRepository.restoreUser(userId)

        accountDeletionRequestRepository.deleteByUserId(userId)

        val accessToken = jwtUtil.generateAccessToken(userId)
        val refreshToken = jwtUtil.generateRefreshToken(userId)
        val expiryDate = Instant.now().plus(14, ChronoUnit.DAYS)
        refreshTokenRepository.save(
            RefreshToken(
                token = refreshToken,
                expiryDate = expiryDate,
                userId = userId,
                deviceToken = request.deviceToken
            )
        )

        val adminIds = userRepository.findAllAdminIds()
        if (adminIds.isNotEmpty()) {
            val notifications = adminIds.map { adminId ->
                NotificationDetails(
                    userId = adminId,
                    subject = "إعادة تفعيل حساب",
                    message = "قام ${deletedUser.getFullName()} بإعادة تفعيل حسابه بنفسه.",
                    type = NotificationType.ALERT,
                    medium = NotificationMedium.PUSH,
                    dataPayload = mapOf("action" to NotificationAction.ACCOUNT_REACTIVATED.name, "targetUserId" to userId.toString())
                )
            }
            eventPublisher.publish(UserNotificationsEvent(notifications))
        }

        return AuthResponse(accessToken, refreshToken)
    }

    @Transactional(readOnly = true)
    fun getDeletionRequests(adminId: UUID, pageable: Pageable): Page<AccountDeletionRequestResponse> {
        val role = userRepository.findRoleById(adminId)
        if (role != UserRole.ADMIN) {
            throw UnauthorizedException("Only admins can access deletion requests")
        }

        val page = accountDeletionRequestRepository.findAllPaged(pageable)
        return page.map { req ->
            AccountDeletionRequestResponse(
                id = req.getId(),
                userId = req.getUserId(),
                userName = req.getUserName(),
                userCode = req.getUserCode(),
                userImageUrl = req.getUserImageUrl(),
                userRole = req.getUserRole(),
                reason = req.getReason(),
                requestedAt = req.getRequestedAt()
            )
        }
    }

    @Transactional(readOnly = true)
    fun getDeletionRequestCount(adminId: UUID): Long {
        val role = userRepository.findRoleById(adminId)
        if (role != UserRole.ADMIN) {
            throw UnauthorizedException("Only admins can access deletion requests count")
        }
        return accountDeletionRequestRepository.countAll()
    }

    @Transactional
    fun approveDeletion(adminId: UUID, requestId: UUID) {
        val role = userRepository.findRoleById(adminId)
        if (role != UserRole.ADMIN) {
            throw UnauthorizedException("Only admins can approve deletion requests")
        }

        val affected = accountDeletionRequestRepository.deleteByRequestId(requestId)
        if (affected == 0) {
            throw EntityNotFoundException("Deletion request not found")
        }
    }

    @Transactional
    fun rejectDeletion(adminId: UUID, requestId: UUID) {
        val role = userRepository.findRoleById(adminId)
        if (role != UserRole.ADMIN) {
            throw UnauthorizedException("Only admins can reject deletion requests")
        }

        val request = accountDeletionRequestRepository.findProjectionById(requestId)
            ?: throw EntityNotFoundException("Deletion request not found")

        val targetUserId = request.getUserId()
        userRepository.restoreUser(targetUserId)

        accountDeletionRequestRepository.deleteByRequestId(requestId)

        val adminIds = userRepository.findAllAdminIds()
        if (adminIds.isNotEmpty()) {
            val notifications = adminIds.map { aId ->
                NotificationDetails(
                    userId = aId,
                    subject = "استعادة حساب",
                    message = "تمت استعادة وتفعيل حساب ${request.getUserName()} بواسطة ادمن.",
                    type = NotificationType.ALERT,
                    medium = NotificationMedium.PUSH,
                    dataPayload = mapOf("action" to NotificationAction.ACCOUNT_RESTORED_BY_ADMIN.name, "targetUserId" to targetUserId.toString())
                )
            }
            eventPublisher.publish(UserNotificationsEvent(notifications))
        }
    }
}
