package org.teEcclesia.identity.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.teEcclesia.identity.api.dto.request.ParentProfileRequest
import org.teEcclesia.identity.entity.ParentProfile
import org.teEcclesia.identity.entity.User
import org.teEcclesia.identity.exception.ResourceNotFoundException
import org.teEcclesia.identity.repository.ParentProfileRepository
import org.teEcclesia.identity.repository.UserRepository

@Service
class ParentProfileService(
    private val userRepository: UserRepository,
    private val parentProfileRepository: ParentProfileRepository
) {
    @Transactional
    fun createOrUpdateProfile(user: User, request: ParentProfileRequest): ParentProfile {
        val parentProfile = user.parentProfile ?: ParentProfile(user = user)

        // Handle Partner
        if (!request.partnerCode.isNullOrBlank()) {
            val partner = userRepository.findByCode(request.partnerCode)
                ?: throw ResourceNotFoundException("Partner code ${request.partnerCode} is invalid")
            parentProfile.partner = partner
        } else {
            parentProfile.partner = null
        }

        // Handle Children
        if (!request.childrenCodes.isNullOrEmpty()) {
            val children = userRepository.findAllByCodeIn(request.childrenCodes)
            if (children.size != request.childrenCodes.size) {
                throw ResourceNotFoundException("One or more children codes are invalid")
            }
            parentProfile.children.clear()
            parentProfile.children.addAll(children)
        } else {
            parentProfile.children.clear()
        }

        return parentProfileRepository.save(parentProfile)
    }

    @Transactional
    fun syncPartner(parentProfile: ParentProfile) {
        val partner = parentProfile.partner ?: return
        val partnerProfile = partner.parentProfile ?: ParentProfile(user = partner)
        
        // Link the partner back to this user
        partnerProfile.partner = parentProfile.user
        
        // Merge children
        val allChildren = (parentProfile.children + partnerProfile.children).distinctBy { it.id }
        
        parentProfile.children.clear()
        parentProfile.children.addAll(allChildren)
        
        partnerProfile.children.clear()
        partnerProfile.children.addAll(allChildren)

        parentProfileRepository.save(parentProfile)
        parentProfileRepository.save(partnerProfile)
    }
}
