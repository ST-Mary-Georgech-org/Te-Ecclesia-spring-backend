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
import org.teEcclesia.identity.api.dto.request.ApproveUserRequest
import org.teEcclesia.identity.api.dto.response.InitiateWhatsAppVerificationResponse
import org.teEcclesia.identity.api.dto.response.toProfileResponse
import org.teEcclesia.identity.exception.UnauthorizedException
import org.teEcclesia.identity.entity.enums.UserStatus
import org.teEcclesia.identity.entity.enums.UserRole
import org.teEcclesia.identity.entity.toUserUpdatedEvent
import org.teEcclesia.identity.exception.UserAlreadyExistsException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.teEcclesia.identity.api.dto.request.RegisterRequest
import org.teEcclesia.identity.utils.extractBirthDate
import org.teEcclesia.identity.utils.extractGender
import org.teEcclesia.identity.api.dto.request.toEntity
import org.teEcclesia.identity.entity.MakhdoomProfile
import org.teEcclesia.identity.repository.EducationalStageRepository
import org.teEcclesia.identity.repository.EducationalYearRepository
import org.teEcclesia.identity.repository.AreaRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.teEcclesia.identity.api.dto.request.ParentProfileRequest
import org.teEcclesia.identity.entity.lookups.Area
import org.teEcclesia.identity.utils.formatHomePhone
import org.teEcclesia.identity.utils.formatPhone
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
    private val authService: AuthService,
    @param:Value("\${storage.teEcclesia.cdn-endpoint}") private val cdnEndpoint: String,
    @param:Value("\${identity.resources.profile-image-directory}") private val profileImageDirectory: String,
    @param:Value("\${identity.resources.documents-directory}") private val documentsDirectory: String
) {
    private val imagesBaseUrl: String = "$cdnEndpoint/$profileImageDirectory"
    fun existById(userId: UUID): Boolean = userRepository.existsById(userId)

    fun findById(userId: UUID): User {
        return userRepository.findByIdOrNull(userId)
            ?: throw UserNotFoundException("User with id: $userId not found")
    }

    fun findProfileById(userId: UUID): User {
        return userRepository.findProfileById(userId)
            ?: throw UserNotFoundException("User with id: $userId not found")
    }

    @Transactional(readOnly = true)
    fun getUserProfile(userId: UUID, imageBaseUrl: String): ProfileResponse {
        val user = findProfileById(userId)
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
            email = request.email
        )

        val savedUser = userRepository.save(updatedUser)
        eventPublisher.publish(savedUser.toUserUpdatedEvent())
    }

    fun initiatePhoneChange(userId: UUID, phone: String): InitiateWhatsAppVerificationResponse {
        val user = findById(userId)
        val formattedPhone = formatPhone(phone)

        if (formattedPhone == user.phone) {
            throw IllegalArgumentException("New phone number must be different from current phone number.")
        }

        val existingUsersByPhone = userRepository.findUsersByPhone(formattedPhone)
        val otherVerifiedPhoneUsers = existingUsersByPhone.filter { it.id != userId && it.isPhoneVerified }

        if (otherVerifiedPhoneUsers.size >= 2) {
            throw UserAlreadyExistsException("Phone number is already registered and verified twice.")
        }

        if (otherVerifiedPhoneUsers.any { it.nationalId == user.nationalId }) {
            throw UserAlreadyExistsException("National ID is already registered.")
        }

        val (deepLink, token) = authService.initiateWhatsAppPhoneChangeVerification(user, formattedPhone)
        return InitiateWhatsAppVerificationResponse(deepLink, token)
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

    @Transactional(readOnly = true)
    fun getUsersByStatus(status: UserStatus, stageId: Long?, yearId: Long?, role: UserRole?, search: String?, pageable: Pageable): Page<ProfileResponse> {
        return userRepository.findByStatusAndFilters(status, stageId, yearId, role, search, pageable).map { it.toProfileResponse(imagesBaseUrl) }
    }

    @Transactional
    fun approveUser(userId: UUID, request: ApproveUserRequest?) {
        var user = findById(userId)
        
        request?.updateProfileData?.let { updateData ->
            val formattedPhone = formatPhone(updateData.phone)
            if (formattedPhone != user.phone || updateData.nationalId != user.nationalId) {
                val existingUsersByPhone = userRepository.findUsersByPhone(formattedPhone)
                val otherVerifiedPhoneUsers = existingUsersByPhone.filter { it.id != user.id && it.isPhoneVerified }

                if (otherVerifiedPhoneUsers.size >= 2) {
                    throw UserAlreadyExistsException("Phone number is already registered and verified twice.")
                }

                if (otherVerifiedPhoneUsers.any { it.nationalId == updateData.nationalId }) {
                    throw UserAlreadyExistsException("National ID is already registered.")
                }

                val existingByNationalId = userRepository.findByNationalId(updateData.nationalId)
                if (existingByNationalId != null && existingByNationalId.id != user.id) {
                    throw UserAlreadyExistsException("National ID is already registered.")
                }
            }

            user = user.copy(
                firstName = updateData.firstName,
                secondName = updateData.secondName,
                thirdName = updateData.thirdName,
                lastName = updateData.lastName,
                displayName = updateData.displayName,
                nationalId = updateData.nationalId,
                phone = formattedPhone,
                isPhoneVerified = if (formattedPhone != user.phone) false else user.isPhoneVerified,
                email = updateData.email,
                job = updateData.job,
                buildingNo = updateData.buildingNo,
                street = updateData.street,
                streetBranch = updateData.streetBranch,
                area = updateData.area,
                floor = updateData.floor,
                apartment = updateData.apartment,
                specialMark = updateData.specialMark,
                externalConfessionPriestName = updateData.externalConfessionPriestName,
                externalConfessionChurch = updateData.externalConfessionChurch,
                externalConfessionPhone = updateData.externalConfessionPhone
            )
        }

        val code = request?.customCode ?: user.code ?: userCodeGenerator.generateCode(user)
        
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

    fun isResponsibleFor(caller: User, target: User): Boolean {
        if (caller.role == UserRole.ADMIN) return true
        
        val callerKhadem = caller.khademProfile ?: return false
        val targetStageId = target.makhdoomProfile?.educationalStage?.id ?: target.khademProfile?.educationalStage?.id
        val targetYearId = target.makhdoomProfile?.educationalYear?.id ?: target.khademProfile?.educationalYear?.id
        
        if (targetStageId != null && callerKhadem.responsibleStages.any { it.id == targetStageId }) return true
        if (targetYearId != null && callerKhadem.responsibleYears.any { it.id == targetYearId }) return true
        
        return false
    }

    @Transactional
    fun updateCode(callerId: UUID, userId: UUID, newCode: String) {
        val caller = findById(callerId)
        val user = findById(userId)
        if (!isResponsibleFor(caller, user)) {
            throw UnauthorizedException("User is not authorized to edit this profile")
        }
        
        val existingUser = userRepository.findByCode(newCode)
        if (existingUser != null && existingUser.id != userId) {
            throw IllegalArgumentException("Code already exists for another user")
        }

        val savedUser = userRepository.save(user.copy(code = newCode))
        eventPublisher.publish(savedUser.toUserUpdatedEvent())
    }

    @Transactional
    fun createMakhdoomDirectly(callerId: UUID, request: RegisterRequest, image: MultipartFile? = null, identityDocument: MultipartFile? = null): ProfileResponse {
        val caller = findById(callerId)
        if (caller.role == UserRole.KHADEM) {
            val khademProfile = caller.khademProfile
            val reqStageId = request.makhdoomProfile?.educationalStageId
            val reqYearId = request.makhdoomProfile?.educationalYearId
            
            if (khademProfile != null) {
                val hasStage = reqStageId != null && khademProfile.responsibleStages.any { it.id == reqStageId }
                val hasYear = reqYearId != null && khademProfile.responsibleYears.any { it.id == reqYearId }
                
                if (!hasStage && !hasYear) {
                    throw UnauthorizedException("You are not responsible for this educational stage or year")
                }
            }
        }

        var confessionPriest: User? = null
        if (request.confessionPriestId != null) {
            confessionPriest = findById(request.confessionPriestId)
        }

        val rawPassword = request.password
        val encodedPassword = passwordEncoder.encode(rawPassword)!!
        val userId = UUID.randomUUID()
        
        val finalImageUrl = if (image != null) {
            imageStorageService.uploadImage(image, userId.toString(), profileImageDirectory)
        } else {
            request.imageUrl
        }

        val existingByNationalId = userRepository.findByNationalId(request.nationalId)
        if (existingByNationalId != null) {
            throw UserAlreadyExistsException("National ID is already registered.")
        }

        val formattedPhone = formatPhone(request.phone)
        val existingUsersByPhone = userRepository.findUsersByPhone(formattedPhone)
        val verifiedPhoneUsers = existingUsersByPhone.filter { it.isPhoneVerified }

        if (verifiedPhoneUsers.size >= 2) {
            throw UserAlreadyExistsException("Phone number is already registered and verified twice.")
        }

        if (verifiedPhoneUsers.any { it.nationalId == request.nationalId }) {
            throw UserAlreadyExistsException("National ID is already registered.")
        }

        val userEntity = request.toEntity(
            hashedPassword = encodedPassword, 
            confessionPriest = confessionPriest,
            id = userId,
            imageUrl = finalImageUrl
        )
        
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
            val finalDocumentUrl = if (identityDocument != null) {
                imageStorageService.uploadImage(identityDocument, "doc_${userId}", documentsDirectory)
            } else {
                request.makhdoomProfile.identityDocumentImageUrl
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
                isMotherDeceased = request.makhdoomProfile.isMotherDeceased,
                identityDocumentImageUrl = finalDocumentUrl
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
    fun createParentDirectly(request: RegisterRequest, image: MultipartFile? = null, nationalIdImage: MultipartFile? = null): ProfileResponse {
        var confessionPriest: User? = null
        if (request.confessionPriestId != null) {
            confessionPriest = findById(request.confessionPriestId)
        }

        val rawPassword = request.password
        val encodedPassword = passwordEncoder.encode(rawPassword)!!
        val userId = UUID.randomUUID()

        val finalImageUrl = if (image != null) {
            imageStorageService.uploadImage(image, userId.toString(), profileImageDirectory)
        } else {
            request.imageUrl
        }

        val existingByNationalId = userRepository.findByNationalId(request.nationalId)
        if (existingByNationalId != null) {
            throw UserAlreadyExistsException("National ID is already registered.")
        }

        val formattedPhone = formatPhone(request.phone)
        val existingUsersByPhone = userRepository.findUsersByPhone(formattedPhone)
        val verifiedPhoneUsers = existingUsersByPhone.filter { it.isPhoneVerified }

        if (verifiedPhoneUsers.size >= 2) {
            throw UserAlreadyExistsException("Phone number is already registered and verified twice.")
        }

        if (verifiedPhoneUsers.any { it.nationalId == request.nationalId }) {
            throw UserAlreadyExistsException("National ID is already registered.")
        }

        val userEntity = request.toEntity(
            hashedPassword = encodedPassword, 
            confessionPriest = confessionPriest,
            id = userId,
            imageUrl = finalImageUrl
        )
        
        val savedUser = userRepository.save(userEntity.copy(
            status = UserStatus.APPROVED, // Direct creations by Khadem are automatically approved
            role = UserRole.PARENT,
            isPhoneVerified = true,
            isEmailVerified = true,
            code = userCodeGenerator.generateCode(userEntity)
        ))

        val finalDocumentUrl = if (nationalIdImage != null) {
            imageStorageService.uploadImage(nationalIdImage, "doc_${userId}", documentsDirectory)
        } else {
            request.parentProfile?.nationalIdImageUrl
        }

        request.parentProfile?.let {
            val parentProfile = parentProfileService.createOrUpdateProfile(savedUser, it)
            parentProfile.nationalIdImageUrl = finalDocumentUrl
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

    @Transactional
    fun updateMakhdoomProfileByKhadem(callerId: UUID, targetUserId: UUID, request: RegisterRequest) {
        val caller = findById(callerId)
        val target = findById(targetUserId)
        if (!isResponsibleFor(caller, target)) {
            throw UnauthorizedException("User is not authorized to edit this profile")
        }

        if (caller.role == UserRole.KHADEM) {
            val khademProfile = caller.khademProfile
            val reqStageId = request.makhdoomProfile?.educationalStageId
            val reqYearId = request.makhdoomProfile?.educationalYearId
            
            if (khademProfile != null) {
                val hasStage = reqStageId != null && khademProfile.responsibleStages.any { it.id == reqStageId }
                val hasYear = reqYearId != null && khademProfile.responsibleYears.any { it.id == reqYearId }
                
                if (!hasStage && !hasYear) {
                    throw UnauthorizedException("You are not responsible for this educational stage or year")
                }
            }
        }

        val formattedPhone = formatPhone(request.phone)
        if (formattedPhone != target.phone) {
            val existingUsersByPhone = userRepository.findUsersByPhone(formattedPhone)
            val otherVerifiedPhoneUsers = existingUsersByPhone.filter { it.id != targetUserId && it.isPhoneVerified }

            if (otherVerifiedPhoneUsers.size >= 2) {
                throw UserAlreadyExistsException("Phone number is already registered and verified twice.")
            }

            if (otherVerifiedPhoneUsers.any { it.nationalId == request.nationalId }) {
                throw UserAlreadyExistsException("National ID is already registered.")
            }
        }

        if (request.nationalId != target.nationalId) {
            val existingByNationalId = userRepository.findByNationalId(request.nationalId)
            if (existingByNationalId != null && existingByNationalId.id != targetUserId) {
                throw UserAlreadyExistsException("National ID is already registered.")
            }
        }

        var confessionPriest: User? = target.confessionPriest
        if (request.confessionPriestId != null && request.confessionPriestId != target.confessionPriest?.id) {
            confessionPriest = findById(request.confessionPriestId)
        }

        val passwordHash = if (!request.password.isNullOrBlank()) {
            passwordEncoder.encode(request.password)!!
        } else {
            target.passwordHash
        }

        val updatedUser = target.copy(
            firstName = request.firstName,
            secondName = request.secondName,
            thirdName = request.thirdName,
            lastName = request.lastName,
            displayName = request.displayName,
            nationalId = request.nationalId,
            phone = formattedPhone,
            homePhone = formatHomePhone(request.homePhone),
            email = request.email,
            passwordHash = passwordHash,
            birthDate = extractBirthDate(request.nationalId),
            job = request.job,
            buildingNo = request.buildingNo,
            street = request.street,
            streetBranch = request.streetBranch,
            area = request.area,
            floor = request.floor,
            apartment = request.apartment,
            specialMark = request.specialMark,
            gender = extractGender(request.nationalId),
            confessionPriest = confessionPriest,
            externalConfessionPriestName = request.externalConfessionPriestName,
            externalConfessionChurch = request.externalConfessionChurch,
            externalConfessionPhone = request.externalConfessionPhone
        )

        val finalUser = if (target.role == UserRole.MAKHDOOM && request.makhdoomProfile != null) {
            val educationalStage = educationalStageRepository.findById(request.makhdoomProfile.educationalStageId).orElseThrow {
                java.lang.IllegalArgumentException("Educational stage not found")
            }
            val educationalYear = request.makhdoomProfile.educationalYearId?.let {
                educationalYearRepository.findById(it).orElseThrow {
                    java.lang.IllegalArgumentException("Educational year not found")
                }
            }

            val currentProfile = target.makhdoomProfile
            val updatedMakhdoomProfile = if (currentProfile != null) {
                currentProfile.copy(
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
            } else {
                MakhdoomProfile(
                    user = updatedUser,
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
            }
            updatedUser.copy(makhdoomProfile = updatedMakhdoomProfile)
        } else {
            updatedUser
        }

        val savedUser = userRepository.save(finalUser)
        eventPublisher.publish(savedUser.toUserUpdatedEvent())
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