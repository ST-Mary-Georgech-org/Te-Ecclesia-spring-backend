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
import org.teEcclesia.identity.entity.enums.UserStatus
import org.teEcclesia.identity.entity.enums.UserRole
import org.teEcclesia.identity.entity.toUserUpdatedEvent
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.teEcclesia.identity.api.dto.request.RegisterRequest
import org.teEcclesia.identity.api.dto.request.toEntity
import org.teEcclesia.identity.entity.MakhdoomProfile
import org.teEcclesia.identity.repository.EducationalStageRepository
import org.teEcclesia.identity.repository.EducationalYearRepository
import org.teEcclesia.identity.repository.AreaRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.teEcclesia.identity.api.dto.request.ParentProfileRequest
import org.teEcclesia.identity.entity.lookups.Area
import java.util.*

@Service
class UserService(
    private val userRepository: UserRepository,
    private val imageStorageService: ImageStorageService,
    private val eventPublisher: TeEcclesiaEventPublisher,
    private val parentProfileService: ParentProfileService,
    private val userCodeGenerator: UserCodeGenerator,
    private val passwordEncoder: PasswordEncoder,
    private val educationalStageRepository: EducationalStageRepository,
    private val educationalYearRepository: EducationalYearRepository,
    private val areaRepository: AreaRepository,
    @param:Value("\${storage.teEcclesia.cdn-endpoint}") private val cdnEndpoint: String,
    @param:Value("\${identity.resources.profile-image-directory}") private val profileImageDirectory: String
) {
    private val imagesBaseUrl: String = "$cdnEndpoint/$profileImageDirectory"
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
            firstName = request.firstName,
            secondName = request.secondName,
            thirdName = request.thirdName,
            lastName = request.lastName,
            displayName = request.displayName,
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

    fun getUsersByStatus(status: UserStatus, pageable: Pageable): Page<ProfileResponse> {
        return userRepository.findByStatus(status, pageable).map { it.toProfileResponse(imagesBaseUrl) }
    }

    @Transactional
    fun approveUser(userId: UUID) {
        val user = findById(userId)
        val code = user.code ?: userCodeGenerator.generateCode(user)
        
        val updatedUser = user.copy(
            status = UserStatus.APPROVED,
            code = code
        )
        val savedUser = userRepository.save(updatedUser)
        
        savedUser.parentProfile?.let {
            parentProfileService.syncPartner(it)
        }
        
        addAreaIfNotExists(savedUser.area)
        
        eventPublisher.publish(savedUser.toUserUpdatedEvent())
    }

    @Transactional
    fun rejectUser(userId: UUID, reason: String) {
        val user = findById(userId)
        if (user.status != UserStatus.PENDING_APPROVAL) {
            throw RuntimeException("Only users pending approval can be rejected")
        }
        val updatedUser = userRepository.save(user.copy(status = UserStatus.REJECTED, statusReason = reason))
        eventPublisher.publish(updatedUser.toUserUpdatedEvent())
    }

    @Transactional
    fun banUser(userId: UUID, reason: String) {
        val user = findById(userId)
        val updatedUser = userRepository.save(user.copy(status = UserStatus.BANNED, statusReason = reason))
        eventPublisher.publish(updatedUser.toUserUpdatedEvent())
    }

    @Transactional
    fun createMakhdoomDirectly(request: RegisterRequest): ProfileResponse {
        var confessionPriest: User? = null
        if (request.confessionPriestId != null) {
            confessionPriest = findById(request.confessionPriestId)
        }

        val rawPassword = request.password
        val encodedPassword = passwordEncoder.encode(rawPassword)!!
        val userEntity = request.toEntity(encodedPassword, confessionPriest)
        
        // Add Makhdoom profile
        val makhdoomProfile = if (request.makhdoomProfile != null) {
            val educationalStage = educationalStageRepository.findById(request.makhdoomProfile.educationalStageId).orElseThrow {
                java.lang.IllegalArgumentException("Educational stage not found")
            }
            val educationalYear = request.makhdoomProfile.educationalYearId?.let {
                educationalYearRepository.findById(it).orElseThrow {
                    java.lang.IllegalArgumentException("Educational year not found")
                }
            }
            MakhdoomProfile(
                user = userEntity,
                shamamsaStudyStatus = request.makhdoomProfile.shamamsaStudyStatus,
                educationalStage = educationalStage,
                educationalYear = educationalYear,
                fatherPhone = request.makhdoomProfile.fatherPhone,
                fatherWhatsapp = request.makhdoomProfile.fatherWhatsapp,
                motherPhone = request.makhdoomProfile.motherPhone,
                motherWhatsapp = request.makhdoomProfile.motherWhatsapp,
                isFatherDeceased = request.makhdoomProfile.isFatherDeceased,
                isMotherDeceased = request.makhdoomProfile.isMotherDeceased
            )
        } else null
        
        val code = userCodeGenerator.generateCode(userEntity)
        val approvedUser: User = userEntity.copy(
            status = UserStatus.APPROVED,
            code = code,
            isPhoneVerified = true,
            makhdoomProfile = makhdoomProfile ?: userEntity.makhdoomProfile
        )
        
        val savedUser = userRepository.save(approvedUser)
        addAreaIfNotExists(savedUser.area)
        eventPublisher.publish(savedUser.toUserUpdatedEvent())
        
        return savedUser.toProfileResponse(imagesBaseUrl)
    }

    @Transactional
    fun createParentDirectly(request: RegisterRequest): ProfileResponse {
        var confessionPriest: User? = null
        if (request.confessionPriestId != null) {
            confessionPriest = findById(request.confessionPriestId)
        }

        val rawPassword = request.password
        val encodedPassword = passwordEncoder.encode(rawPassword)!!
        val userEntity = request.toEntity(encodedPassword, confessionPriest)
        
        val savedUser = userRepository.save(userEntity.copy(
            status = UserStatus.APPROVED, // Direct creations by Khadem are automatically approved
            role = UserRole.PARENT,
            isPhoneVerified = true,
            isEmailVerified = true,
            code = userCodeGenerator.generateCode(userEntity)
        ))

        request.parentProfile?.let {
            val parentProfile = parentProfileService.createOrUpdateProfile(savedUser, it)
            savedUser.parentProfile = parentProfile
            userRepository.save(savedUser)
            parentProfileService.syncPartner(parentProfile)
        }

        addAreaIfNotExists(savedUser.area)
        eventPublisher.publish(savedUser.toUserUpdatedEvent())
        return savedUser.toProfileResponse(imagesBaseUrl)
    }

    @Transactional
    fun updateParentProfile(parentId: UUID, request: ParentProfileRequest): ProfileResponse {
        val user = findById(parentId)
        if (user.role != UserRole.PARENT) {
            throw IllegalArgumentException("User is not a PARENT")
        }

        val parentProfile = parentProfileService.createOrUpdateProfile(user, request)
        user.parentProfile = parentProfile
        val savedUser = userRepository.save(user)
        
        if (savedUser.status == UserStatus.APPROVED) {
            parentProfileService.syncPartner(parentProfile)
        }

        eventPublisher.publish(savedUser.toUserUpdatedEvent())
        return savedUser.toProfileResponse(imagesBaseUrl)
    }

    private fun addAreaIfNotExists(areaName: String) {
        val area = areaName.trim()
        if (area.isNotEmpty()) {
            val existing = areaRepository.findByName(area)
            if (existing == null) {
                areaRepository.save(Area(name = area, suggestedCount = 1))
            }
        }
    }
}