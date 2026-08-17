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
import org.teEcclesia.identity.api.dto.response.toUserSummaryResponse
import org.teEcclesia.identity.exception.UnauthorizedException
import org.teEcclesia.identity.entity.enums.UserStatus
import org.teEcclesia.events.notifications.UserNotificationsEvent
import org.teEcclesia.events.notifications.NotificationDetails
import org.teEcclesia.events.notifications.utils.NotificationMedium
import org.teEcclesia.events.notifications.utils.NotificationType
import org.teEcclesia.identity.entity.enums.UserRole
import org.teEcclesia.identity.entity.toUserUpdatedEvent
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.teEcclesia.identity.api.dto.request.RegisterRequest
import java.time.Instant
import org.teEcclesia.identity.utils.extractBirthDate
import org.teEcclesia.identity.utils.extractGender
import org.teEcclesia.identity.api.dto.request.toEntity
import org.teEcclesia.identity.entity.MakhdoomProfile
import org.teEcclesia.identity.entity.KhademProfile
import org.teEcclesia.identity.entity.OrdinationProfile
import org.teEcclesia.identity.entity.KahenProfile
import org.teEcclesia.identity.repository.EducationalStageRepository
import org.teEcclesia.identity.repository.EducationalYearRepository
import org.teEcclesia.identity.repository.AreaRepository
import org.teEcclesia.identity.repository.RankRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.teEcclesia.identity.api.dto.request.ParentProfileRequest
import org.teEcclesia.identity.api.dto.response.toUserSummaryResponseWithFullName
import org.teEcclesia.identity.entity.lookups.Area
import org.teEcclesia.identity.entity.enums.SettingKey
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
    private val rankRepository: RankRepository,
    private val authService: AuthService,
    private val userValidationHelper: UserValidationHelper,
    private val systemSettingService: SystemSettingService,
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

    private fun getParentsWhatsAppLink(): String? {
        return systemSettingService.getSettingValue(SettingKey.PARENTS_WHATSAPP_LINK)
    }

    @Transactional(readOnly = true)
    fun getUserProfile(userId: UUID, imageBaseUrl: String): ProfileResponse {
        val user = findProfileById(userId)
        return user.toProfileResponse(imageBaseUrl, getParentsWhatsAppLink())
    }

    @Transactional(readOnly = true)
    fun getUserProfile(userId: UUID): ProfileResponse {
        val user = findProfileById(userId)
        return user.toProfileResponse(imagesBaseUrl, getParentsWhatsAppLink())
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

        if (!request.email.isNullOrBlank() && !request.email.equals(user.email, ignoreCase = true)) {
            userValidationHelper.validateEmail(request.email, currentUserId = userId)
        }

        val updatedUser = user.copy(
            firstName = request.firstName,
            secondName = request.secondName,
            thirdName = request.thirdName,
            lastName = request.lastName,
            displayName = request.displayName,
            email = request.email,
            isEmailVerified = if (!request.email.isNullOrBlank() && request.email.equals(user.email, ignoreCase = true)) user.isEmailVerified else false
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

        userValidationHelper.validatePhone(phone = phone, nationalId = user.nationalId, currentUserId = userId)


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
    fun getUsersByStatus(callerId: UUID, status: UserStatus, stageId: Long?, yearId: Long?, role: UserRole?, search: String?, pageable: Pageable): Page<ProfileResponse> {
        val caller = findProfileById(callerId)
        var stageIdsToQuery: Collection<Long>? = if (stageId != null) listOf(stageId) else null
        var yearIdsToQuery: Collection<Long>? = if (yearId != null) listOf(yearId) else null

        if (caller.role == UserRole.KHADEM) {
            val khademProfile = caller.khademProfile
                ?: throw UnauthorizedException("Khadem profile not found")

            val canApprove = khademProfile.canApproveRequests

            if (status != UserStatus.APPROVED && !canApprove) {
                throw UnauthorizedException("User does not have permission to view non-approved requests")
            }

            if (!canApprove) {
                val allowedStageIds = mutableSetOf<Long>()
                val allowedYearIds = mutableSetOf<Long>()

                allowedStageIds.add(khademProfile.educationalStage.id)
                khademProfile.educationalYear?.id?.let { allowedYearIds.add(it) }
                allowedStageIds.addAll(khademProfile.responsibleStages.map { it.id })
                allowedYearIds.addAll(khademProfile.responsibleYears.map { it.id })

                if (stageId != null && !allowedStageIds.contains(stageId)) {
                    throw UnauthorizedException("Khadem does not have permission to view this stage")
                }
                if (yearId != null && !allowedYearIds.contains(yearId)) {
                    throw UnauthorizedException("Khadem does not have permission to view this year")
                }

                if (stageId == null) {
                    stageIdsToQuery = allowedStageIds
                }
                if (yearId == null && allowedYearIds.isNotEmpty()) {
                    yearIdsToQuery = allowedYearIds
                }
            }
        } else if (caller.role != UserRole.ADMIN) {
            throw UnauthorizedException("Only Admin or Khadem can view users")
        }

        val whatsappLink = getParentsWhatsAppLink()

        val profiles = userRepository.findProfilesByStatusAndFiltersProjection(
            status = status,
            stageIds = stageIdsToQuery,
            yearIds = yearIdsToQuery,
            role = role,
            search = search,
            pageable = pageable
        )
        
        val parentIds = profiles.mapNotNull { it.getParentProfile()?.getId() }
        val childrenByParentId = if (parentIds.isNotEmpty()) {
            userRepository.findChildrenByParentIds(parentIds).groupBy(
                { it.getParentId() },
                { it.toUserSummaryResponseWithFullName(imagesBaseUrl) }
            )
        } else {
            emptyMap()
        }

        return profiles.map { it.toProfileResponse(imagesBaseUrl, whatsappLink, childrenByParentId) }
    }

    @Transactional
    fun approveUser(
        userId: UUID, 
        request: ApproveUserRequest?,
        image: MultipartFile? = null,
        identityDocument: MultipartFile? = null,
        ordinationCertificate: MultipartFile? = null
    ) {
        var user = findById(userId)
        
        if (user.status == UserStatus.APPROVED) {
            throw RuntimeException("User is already approved")
        }
        
        if (user.isEmailVerified) {
            userValidationHelper.validateEmail(user.email, currentUserId = user.id)
        }
        
        if (request?.updateProfileData != null) {
            user = updateUserData(user, request.updateProfileData)
        }
        
        val finalImageUrl = if (image != null) {
            imageStorageService.uploadImage(image, user.id.toString(), profileImageDirectory)
        } else user.imageUrl
        
        val finalDocumentUrl = if (identityDocument != null) {
            imageStorageService.uploadImage(identityDocument, "doc_${user.id}", documentsDirectory)
        } else user.makhdoomProfile?.identityDocumentImageUrl
        
        val finalCertificateUrl = if (ordinationCertificate != null) {
            imageStorageService.uploadImage(ordinationCertificate, "cert_${user.id}", documentsDirectory)
        } else user.ordinationProfile?.certificateImageUrl
        
        user = user.copy(
            imageUrl = finalImageUrl,
            makhdoomProfile = user.makhdoomProfile?.copy(identityDocumentImageUrl = finalDocumentUrl),
            ordinationProfile = user.ordinationProfile?.copy(certificateImageUrl = finalCertificateUrl)
        )

        val code = request?.customCode ?: user.code ?: userCodeGenerator.generateCode(user)
        
        val updatedUser = user.copy(
            status = UserStatus.APPROVED,
            code = code,
            isPhoneVerified = true,
            actionTakenAt = Instant.now()
        )
        val savedUser = userRepository.save(updatedUser)
        
        savedUser.parentProfile?.let {
            parentProfileService.syncPartner(savedUser)
        }
        
        addAreaIfNotExists(savedUser.area)
        
        eventPublisher.publish(savedUser.toUserUpdatedEvent())

        eventPublisher.publish(
            UserNotificationsEvent(
                listOf(
                    NotificationDetails(
                        userId = savedUser.id,
                        subject = "تم تفعيل الحساب",
                        message = "تمت الموافقة على حسابك بنجاح. يمكنك الآن استخدام كل مميزات التطبيق.",
                        type = NotificationType.SYSTEM,
                        medium = NotificationMedium.PUSH
                    )
                )
            )
        )
    }

    @Transactional
    fun updateUserByAdminOrKhadem(
        callerId: UUID, 
        targetUserId: UUID, 
        request: ApproveUserRequest,
        image: MultipartFile? = null,
        identityDocument: MultipartFile? = null,
        ordinationCertificate: MultipartFile? = null
    ) {
        val caller = findById(callerId)
        val target = findById(targetUserId)
        
        if (target.isEmailVerified) {
            userValidationHelper.validateEmail(target.email, currentUserId = target.id)
        }
        
        var user = target
        if (request.updateProfileData != null) {
            user = updateUserData(user, request.updateProfileData)
            
            if (!request.updateProfileData.password.isNullOrBlank()) {
                user = user.copy(passwordHash = passwordEncoder.encode(request.updateProfileData.password)!!)
            }
            
            if (request.updateProfileData.role != null && request.updateProfileData.role != target.role) {
                if (caller.role != UserRole.ADMIN) {
                    throw UnauthorizedException("Only admins can change user roles")
                }
                user = user.copy(role = request.updateProfileData.role)
            }
        }
        
        if (caller.role != UserRole.ADMIN) {
            if (!isResponsibleFor(caller, target)) {
                throw UnauthorizedException("User is not authorized to edit this profile")
            }
        }
        
        val finalImageUrl = if (image != null) {
            imageStorageService.uploadImage(image, user.id.toString(), profileImageDirectory)
        } else user.imageUrl
        
        val finalDocumentUrl = if (identityDocument != null) {
            imageStorageService.uploadImage(identityDocument, "doc_${user.id}", documentsDirectory)
        } else user.makhdoomProfile?.identityDocumentImageUrl
        
        val finalCertificateUrl = if (ordinationCertificate != null) {
            imageStorageService.uploadImage(ordinationCertificate, "cert_${user.id}", documentsDirectory)
        } else user.ordinationProfile?.certificateImageUrl
        
        user = user.copy(
            imageUrl = finalImageUrl,
            makhdoomProfile = user.makhdoomProfile?.copy(identityDocumentImageUrl = finalDocumentUrl),
            ordinationProfile = user.ordinationProfile?.copy(certificateImageUrl = finalCertificateUrl)
        )
        
        val code = request.customCode ?: user.code ?: userCodeGenerator.generateCode(user)
        
        val updatedUser = user.copy(
            code = code,
            actionTakenAt = Instant.now()
        )
        
        val savedUser = userRepository.save(updatedUser)
        
        savedUser.parentProfile?.let {
            parentProfileService.syncPartner(savedUser)
        }
        
        addAreaIfNotExists(savedUser.area)
        
        eventPublisher.publish(savedUser.toUserUpdatedEvent())
    }

    private fun updateUserData(originalUser: User, updateData: RegisterRequest): User {
        var user = originalUser
        userValidationHelper.validateUserUniqueness(
            phone = updateData.phone,
            nationalId = updateData.nationalId,
            email = updateData.email,
            currentUserId = user.id
        )
        val formattedPhone = formatPhone(updateData.phone)

        var confessionPriest: User? = user.confessionPriest
        if (updateData.confessionPriestId != null) {
            if (updateData.confessionPriestId != user.confessionPriest?.id) {
                confessionPriest = findById(updateData.confessionPriestId)
            }
        } else if (updateData.externalConfessionPriestName != null) {
            confessionPriest = null
        }

        user = user.copy(
            firstName = updateData.firstName,
            secondName = updateData.secondName,
            thirdName = updateData.thirdName,
            lastName = updateData.lastName,
            displayName = updateData.displayName,
            nationalId = updateData.nationalId,
            phone = formattedPhone,
            homePhone = formatHomePhone(updateData.homePhone),
            isPhoneVerified = if (user.status == UserStatus.APPROVED) true else if (formattedPhone != user.phone) false else user.isPhoneVerified,
            isEmailVerified = if (updateData.email != user.email) false else user.isEmailVerified,
            email = updateData.email,
            imageUrl = updateData.imageUrl ?: user.imageUrl,
            job = updateData.job,
            buildingNo = updateData.buildingNo,
            street = updateData.street,
            streetBranch = updateData.streetBranch,
            area = updateData.area,
            floor = updateData.floor,
            apartment = updateData.apartment,
            specialMark = updateData.specialMark,
            role = updateData.role ?: user.role,
            confessionPriest = confessionPriest,
            externalConfessionPriestName = updateData.externalConfessionPriestName,
            externalConfessionChurch = updateData.externalConfessionChurch,
            externalConfessionPhone = updateData.externalConfessionPhone
        )

        if (updateData.ordinationProfile != null) {
            val rank = rankRepository.findById(updateData.ordinationProfile.rankId).orElseThrow {
                IllegalArgumentException("Rank not found")
            }
            val currentOrdination = user.ordinationProfile
            val updatedOrdination = currentOrdination?.copy(
                rank = rank,
                isOrdinationInAnotherChurch = updateData.ordinationProfile.isOrdinationInAnotherChurch ?: false,
                ordinationYear = updateData.ordinationProfile.ordinationYear,
                bishopName = updateData.ordinationProfile.bishopName,
                ordinationPlace = updateData.ordinationProfile.ordinationPlace,
                certificateImageUrl = updateData.ordinationProfile.certificateImageUrl ?: currentOrdination.certificateImageUrl
            ) ?: OrdinationProfile(
                user = user,
                rank = rank,
                isOrdinationInAnotherChurch = updateData.ordinationProfile.isOrdinationInAnotherChurch ?: false,
                ordinationYear = updateData.ordinationProfile.ordinationYear,
                bishopName = updateData.ordinationProfile.bishopName,
                ordinationPlace = updateData.ordinationProfile.ordinationPlace,
                certificateImageUrl = updateData.ordinationProfile.certificateImageUrl
            )
            user = user.copy(ordinationProfile = updatedOrdination)
        }

        if (user.role == UserRole.MAKHDOOM && updateData.makhdoomProfile != null) {
            val educationalStage = educationalStageRepository.findById(updateData.makhdoomProfile.educationalStageId).orElseThrow {
                IllegalArgumentException("Educational stage not found")
            }
            if (educationalStage.isKhademOnly) {
                throw IllegalArgumentException("Educational stage is reserved for Khadem role")
            }
            val educationalYear = updateData.makhdoomProfile.educationalYearId?.let {
                educationalYearRepository.findById(it).orElseThrow {
                    IllegalArgumentException("Educational year not found")
                }
            }
            val currentProfile = user.makhdoomProfile
            val updatedMakhdoomProfile = currentProfile?.copy(
                shamamsaStudyStatus = updateData.makhdoomProfile.shamamsaStudyStatus,
                educationalStage = educationalStage,
                educationalYear = educationalYear,
                fatherPhone = updateData.makhdoomProfile.fatherPhone,
                fatherWhatsapp = updateData.makhdoomProfile.fatherWhatsapp,
                motherPhone = updateData.makhdoomProfile.motherPhone,
                motherWhatsapp = updateData.makhdoomProfile.motherWhatsapp,
                isFatherDeceased = updateData.makhdoomProfile.isFatherDeceased ?: false,
                isMotherDeceased = updateData.makhdoomProfile.isMotherDeceased ?: false,
                identityDocumentImageUrl = updateData.makhdoomProfile.identityDocumentImageUrl ?: currentProfile.identityDocumentImageUrl
            ) ?: MakhdoomProfile(
                user = user,
                shamamsaStudyStatus = updateData.makhdoomProfile.shamamsaStudyStatus,
                educationalStage = educationalStage,
                educationalYear = educationalYear,
                fatherPhone = updateData.makhdoomProfile.fatherPhone,
                fatherWhatsapp = updateData.makhdoomProfile.fatherWhatsapp,
                motherPhone = updateData.makhdoomProfile.motherPhone,
                motherWhatsapp = updateData.makhdoomProfile.motherWhatsapp,
                isFatherDeceased = updateData.makhdoomProfile.isFatherDeceased ?: false,
                isMotherDeceased = updateData.makhdoomProfile.isMotherDeceased ?: false,
                identityDocumentImageUrl = updateData.makhdoomProfile.identityDocumentImageUrl
            )
            user = user.copy(makhdoomProfile = updatedMakhdoomProfile)
        }

        if (user.role == UserRole.KHADEM) {
            updateData.khademProfile?.let { khademDto ->
                val educationalStage = educationalStageRepository.findById(khademDto.educationalStageId).orElseThrow {
                    IllegalArgumentException("Educational stage not found")
                }
                val educationalYear = khademDto.educationalYearId?.let {
                    educationalYearRepository.findById(it).orElseThrow {
                        IllegalArgumentException("Educational year not found")
                    }
                }
                val respStages = khademDto.responsibleStageIds?.takeIf { it.isNotEmpty() }?.let {
                    educationalStageRepository.findAllById(it)
                } ?: emptyList()
                val respYears = khademDto.responsibleYearIds?.takeIf { it.isNotEmpty() }?.let {
                    educationalYearRepository.findAllById(it)
                } ?: emptyList()
                val currentKhadem = user.khademProfile
                val updatedKhadem = currentKhadem?.copy(
                    educationalStage = educationalStage,
                    educationalYear = educationalYear,
                    canApproveRequests = khademDto.canApproveRequests ?: false,
                    responsibleStages = respStages.toMutableList(),
                    responsibleYears = respYears.toMutableList()
                ) ?: KhademProfile(
                    user = user,
                    educationalStage = educationalStage,
                    educationalYear = educationalYear,
                    canApproveRequests = khademDto.canApproveRequests ?: false,
                    responsibleStages = respStages.toMutableList(),
                    responsibleYears = respYears.toMutableList()
                )
                user = user.copy(khademProfile = updatedKhadem)
            }
            updateData.adminKhademProfile?.let { adminKhademDto ->
                val responsibleStages = adminKhademDto.responsibleStageIds?.takeIf { it.isNotEmpty() }?.let {
                    educationalStageRepository.findAllById(it)
                } ?: emptyList()
                val responsibleYears = adminKhademDto.responsibleYearIds?.takeIf { it.isNotEmpty() }?.let {
                    educationalYearRepository.findAllById(it)
                } ?: emptyList()
                val currentKhadem = user.khademProfile
                if (currentKhadem != null) {
                    val updatedKhadem = currentKhadem.copy(
                        canApproveRequests = adminKhademDto.canApproveRequests ?: false,
                        responsibleStages = responsibleStages.toMutableList(),
                        responsibleYears = responsibleYears.toMutableList()
                    )
                    user = user.copy(khademProfile = updatedKhadem)
                }
            }
        }

        if (user.role == UserRole.PARENT && updateData.parentProfile != null) {
            val updatedParent = parentProfileService.createOrUpdateProfile(user, updateData.parentProfile)
            user = user.copy(parentProfile = updatedParent)
        }

        if (user.role == UserRole.KAHEN && updateData.kahenProfile != null) {
            val stages = if (updateData.kahenProfile.educationalStageIds.isNotEmpty()) {
                educationalStageRepository.findAllById(updateData.kahenProfile.educationalStageIds)
            } else emptyList()
            val currentKahen = user.kahenProfile
            val updatedKahen = currentKahen?.copy(
                educationalStages = stages.toMutableList(),
                ordinationDate = updateData.kahenProfile.ordinationDate
            ) ?: KahenProfile(
                user = user,
                educationalStages = stages.toMutableList(),
                ordinationDate = updateData.kahenProfile.ordinationDate
            )
            user = user.copy(kahenProfile = updatedKahen)
        }

        return user
    }


    @Transactional
    fun rejectUser(userId: UUID, reason: String) {
        val user = findById(userId)
        if (user.status != UserStatus.PENDING_APPROVAL) {
            throw RuntimeException("Only users pending approval can be rejected")
        }
        val updatedUser = userRepository.save(user.copy(status = UserStatus.REJECTED, statusReason = reason, actionTakenAt = Instant.now()))
        eventPublisher.publish(updatedUser.toUserUpdatedEvent())

        eventPublisher.publish(
            UserNotificationsEvent(
                listOf(
                    NotificationDetails(
                        userId = updatedUser.id,
                        subject = "تم رفض طلب التسجيل",
                        message = "تم رفض طلب التسجيل الخاص بك: $reason",
                        type = NotificationType.SYSTEM,
                        medium = NotificationMedium.PUSH
                    )
                )
            )
        )
    }

    @Transactional
    fun banUser(userId: UUID, reason: String) {
        val user = findById(userId)
        val updatedUser = userRepository.save(user.copy(status = UserStatus.BANNED, statusReason = reason, actionTakenAt = Instant.now()))
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
    fun createMakhdoomDirectly(callerId: UUID, request: RegisterRequest, image: MultipartFile? = null, identityDocument: MultipartFile? = null) {
        val caller = findById(callerId)
        if (caller.role == UserRole.KHADEM) {
            val khademProfile = caller.khademProfile
            if (khademProfile == null || (khademProfile.responsibleStages.isEmpty() && khademProfile.responsibleYears.isEmpty())) {
                throw UnauthorizedException("Only Khadems responsible for a stage or year can add a student")
            }
            val reqStageId = request.makhdoomProfile?.educationalStageId
            val reqYearId = request.makhdoomProfile?.educationalYearId
            
            val hasStage = reqStageId != null && khademProfile.responsibleStages.any { it.id == reqStageId }
            val hasYear = reqYearId != null && khademProfile.responsibleYears.any { it.id == reqYearId }
            
            if (!hasStage && !hasYear) {
                throw UnauthorizedException("You are not responsible for this educational stage or year")
            }
        } else if (caller.role != UserRole.ADMIN) {
            throw UnauthorizedException("Only Admin or responsible Khadem can add a student")
        }

        var confessionPriest: User? = null
        if (request.confessionPriestId != null) {
            confessionPriest = findById(request.confessionPriestId)
        }

        val rawPassword = request.password?.ifBlank { null } ?: generateRandomPassword()
        val encodedPassword = passwordEncoder.encode(rawPassword)!!
        val userId = UUID.randomUUID()
        
        val finalImageUrl = if (image != null) {
            imageStorageService.uploadImage(image, userId.toString(), profileImageDirectory)
        } else {
            request.imageUrl
        }

        userValidationHelper.validateUserUniqueness(
            phone = request.phone,
            nationalId = request.nationalId,
            email = request.email
        )


        val userEntity = request.toEntity(
            hashedPassword = encodedPassword, 
            confessionPriest = confessionPriest,
            id = userId,
            imageUrl = finalImageUrl
        )
        
        // Add Makhdoom profile
        val makhdoomProfile = if (request.makhdoomProfile != null) {
            val educationalStage = educationalStageRepository.findById(request.makhdoomProfile.educationalStageId).orElseThrow {
                IllegalArgumentException("Educational stage not found")
            }
            if (educationalStage.isKhademOnly) {
                throw IllegalArgumentException("Educational stage is reserved for Khadem role")
            }
            val educationalYear = request.makhdoomProfile.educationalYearId?.let {
                educationalYearRepository.findById(it).orElseThrow {
                    IllegalArgumentException("Educational year not found")
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
                isFatherDeceased = request.makhdoomProfile.isFatherDeceased ?: false,
                isMotherDeceased = request.makhdoomProfile.isMotherDeceased ?: false,
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
    }

    @Transactional
    fun createParentDirectly(request: RegisterRequest, image: MultipartFile? = null, nationalIdImage: MultipartFile? = null) {
        var confessionPriest: User? = null
        if (request.confessionPriestId != null) {
            confessionPriest = findById(request.confessionPriestId)
        }

        val rawPassword = request.password?.ifBlank { null } ?: generateRandomPassword()
        val encodedPassword = passwordEncoder.encode(rawPassword)!!
        val userId = UUID.randomUUID()

        val finalImageUrl = if (image != null) {
            imageStorageService.uploadImage(image, userId.toString(), profileImageDirectory)
        } else {
            request.imageUrl
        }

        userValidationHelper.validateUserUniqueness(
            phone = request.phone,
            nationalId = request.nationalId,
            email = request.email
        )


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
            val updatedParentProfile = parentProfile.copy(nationalIdImageUrl = finalDocumentUrl)
            val userWithParent = savedUser.copy(parentProfile = updatedParentProfile)
            userRepository.save(userWithParent)
            parentProfileService.syncPartner(userWithParent)
        }

        addAreaIfNotExists(savedUser.area)
        eventPublisher.publish(savedUser.toUserUpdatedEvent())
    }

    @Transactional
    fun updateParentProfile(parentId: UUID, request: ParentProfileRequest) {
        val user = findById(parentId)
        if (user.role != UserRole.PARENT) {
            throw IllegalArgumentException("User is not a PARENT")
        }

        val parentProfile = parentProfileService.createOrUpdateProfile(user, request)
        val updatedUser = user.copy(parentProfile = parentProfile)
        val savedUser = userRepository.save(updatedUser)
        
        if (savedUser.status == UserStatus.APPROVED) {
            parentProfileService.syncPartner(savedUser)
        }

        eventPublisher.publish(savedUser.toUserUpdatedEvent())
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

        userValidationHelper.validateUserUniqueness(
            phone = request.phone,
            nationalId = request.nationalId,
            email = request.email,
            currentUserId = targetUserId
        )
        val formattedPhone = formatPhone(request.phone)


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
                IllegalArgumentException("Educational stage not found")
            }
            if (educationalStage.isKhademOnly) {
                throw IllegalArgumentException("Educational stage is reserved for Khadem role")
            }
            val educationalYear = request.makhdoomProfile.educationalYearId?.let {
                educationalYearRepository.findById(it).orElseThrow {
                    IllegalArgumentException("Educational year not found")
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
                    isFatherDeceased = request.makhdoomProfile.isFatherDeceased ?: false,
                    isMotherDeceased = request.makhdoomProfile.isMotherDeceased ?: false
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
                    isFatherDeceased = request.makhdoomProfile.isFatherDeceased ?: false,
                    isMotherDeceased = request.makhdoomProfile.isMotherDeceased ?: false
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

    private fun generateRandomPassword(): String {
        return UUID.randomUUID().toString().replace("-", "") + "A1@a"
    }
}
