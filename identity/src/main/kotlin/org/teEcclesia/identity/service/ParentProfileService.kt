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
    fun createOrUpdateProfile(user: User, request: ParentProfileRequest, finalIdentityDocumentUrl: String? = null): ParentProfile {
        var profile = user.parentProfile ?: ParentProfile(user = user)

        // Handle Partner
        val partner = if (!request.partnerCode.isNullOrBlank()) {
            userRepository.findByCode(request.partnerCode)
                ?: throw ResourceNotFoundException("Partner code ${request.partnerCode} is invalid")
        } else null

        // Handle Children
        val children = if (!request.childrenCodes.isNullOrEmpty()) {
            val fetchedChildren = userRepository.findAllByCodeIn(request.childrenCodes)
            if (fetchedChildren.size != request.childrenCodes.size) {
                throw ResourceNotFoundException("One or more children codes are invalid")
            }
            fetchedChildren
        } else emptyList()

        val finalNationalIdUrl = finalIdentityDocumentUrl ?: request.nationalIdImageUrl ?: profile.nationalIdImageUrl

        profile = profile.copy(
            partner = partner,
            children = children,
            nationalIdImageUrl = finalNationalIdUrl
        )

        return parentProfileRepository.save(profile)
    }

    @Transactional
    fun syncPartner(parentProfile: ParentProfile) {
        val partner = parentProfile.partner ?: return
        val partnerProfile = partner.parentProfile ?: ParentProfile(user = partner)
        
        // Merge children
        val allChildren = (parentProfile.children + partnerProfile.children).distinctBy { it.id }
        
        val updatedParentProfile = parentProfile.copy(children = allChildren)
        val updatedPartnerProfile = partnerProfile.copy(
            partner = parentProfile.user,
            children = allChildren
        )

        parentProfileRepository.save(updatedParentProfile)
        parentProfileRepository.save(updatedPartnerProfile)
    }
}
