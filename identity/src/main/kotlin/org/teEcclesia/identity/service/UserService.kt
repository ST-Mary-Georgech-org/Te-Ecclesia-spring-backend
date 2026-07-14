package org.teEcclesia.identity.service

import org.teEcclesia.events.publisher.TeEcclesiaEventPublisher
import org.teEcclesia.identity.api.dto.request.UpdateProfileRequest
import org.teEcclesia.identity.api.dto.response.ProfileResponse
import org.teEcclesia.identity.entity.User
import org.teEcclesia.identity.exception.UserNotFoundException
import org.teEcclesia.identity.repository.UserRepository
import org.teEcclesia.storage.service.ImageStorageService
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import org.teEcclesia.identity.api.dto.response.toProfileResponse
import org.teEcclesia.identity.entity.toUserUpdatedEvent
import java.util.*

@Service
class UserService(
    private val userRepository: UserRepository,
    private val imageStorageService: ImageStorageService,
    private val eventPublisher: TeEcclesiaEventPublisher,
    @param:Value("\${identity.resources.profile-image-directory}") private val profileImageDirectory: String
) {
    fun existById(userId: UUID): Boolean = userRepository.existsById(userId)

    fun findById(userId: UUID): User {
        return userRepository.findByIdOrNull(userId)
            ?: throw UserNotFoundException("User with id: $userId not found")
    }

    @Transactional
    fun getUserProfile(userId: UUID, imageBaseUrl: String): ProfileResponse {
        val user = findById(userId)
        return user.toProfileResponse(imageBaseUrl)
    }

    fun updateUserImage(
        userId: UUID,
        imageFile: MultipartFile,
    ): String {
        val user = findById(userId)
        val newImageUrl = imageStorageService.uploadImage(
            file = imageFile,
            fileName = "${user.id}",
            folderName = profileImageDirectory
        )
        val savedUser = userRepository.save(user.copy(imageUrl = newImageUrl))
        eventPublisher.publish(savedUser.toUserUpdatedEvent())
        return newImageUrl
    }

    fun updateProfile(userId: UUID, request: UpdateProfileRequest) {
        val user = findById(userId)

        val updatedUser = user.copy(
            fullName = request.fullName,
            username = request.username,
            phone = request.phone,
            email = request.email,
        )

        val savedUser = userRepository.save(updatedUser)
        eventPublisher.publish(savedUser.toUserUpdatedEvent())
    }

    fun deleteUserImage(userId: UUID) {
        val user = findById(userId)
        user.imageUrl?.let { imageUrl ->
            imageStorageService.deleteImage(
                fileName = imageUrl.substringBefore("?"),
                folderName = profileImageDirectory
            )
            val savedUser = userRepository.save(user.copy(imageUrl = null))
            eventPublisher.publish(savedUser.toUserUpdatedEvent())
        }
    }

    fun findEmailsByUserIds(userIds: List<UUID>): Map<String, String> {
        val users = userRepository.findAllById(userIds)
        return users.associate { it.id.toString() to (it.email ?: "") }
    }
}