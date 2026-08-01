package org.teEcclesia.identity.api.controller

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.teEcclesia.identity.api.dto.request.RegisterRequest
import org.teEcclesia.identity.api.dto.request.ActionReasonRequest
import org.teEcclesia.identity.api.dto.response.ProfileResponse
import org.teEcclesia.identity.entity.enums.UserStatus
import org.teEcclesia.identity.service.UserService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.teEcclesia.identity.api.dto.request.ParentProfileRequest
import org.teEcclesia.identity.api.dto.request.ApproveUserRequest
import org.teEcclesia.identity.api.dto.request.UserCodeRequest
import org.teEcclesia.identity.entity.enums.UserRole
import org.teEcclesia.identity.exception.UnauthorizedException
import org.springframework.http.MediaType
import org.springframework.web.multipart.MultipartFile
import jakarta.validation.Valid
import java.util.UUID

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService
) {

    private fun authorizeAdminOrKhadem(callerId: UUID, requireApprovePermission: Boolean = false) {
        val caller = userService.findProfileById(callerId)
        if (caller.role == UserRole.ADMIN) return
        
        if (caller.role == UserRole.KHADEM) {
            if (requireApprovePermission && caller.khademProfile?.canApproveRequests != true) {
                throw UnauthorizedException("User does not have permission to approve requests")
            }
            return
        }
        
        throw UnauthorizedException("User is not authorized for this action")
    }

    @GetMapping("/status/{status}")
    fun getUsersByStatus(
        @AuthenticationPrincipal callerId: UUID,
        @PathVariable status: UserStatus,
        @RequestParam(required = false) stageId: Long?,
        @RequestParam(required = false) yearId: Long?,
        @RequestParam(required = false) role: UserRole?,
        @RequestParam(required = false) search: String?,
        pageable: Pageable
    ): ResponseEntity<Page<ProfileResponse>> {
        if (status != UserStatus.APPROVED) {
            authorizeAdminOrKhadem(callerId, requireApprovePermission = true)
        }
        return ResponseEntity.ok(userService.getUsersByStatus(callerId, status, stageId, yearId, role, search, pageable))
    }

    @GetMapping("/{userId}")
    fun getUserById(
        @AuthenticationPrincipal callerId: UUID,
        @PathVariable userId: UUID
    ): ResponseEntity<ProfileResponse> {
        authorizeAdminOrKhadem(callerId)
        return ResponseEntity.ok(userService.getUserProfile(userId))
    }


    @PostMapping("/{userId}/approve")
    fun approveUser(
        @AuthenticationPrincipal callerId: UUID, 
        @PathVariable userId: UUID,
        @RequestBody(required = false) request: ApproveUserRequest?
    ): ResponseEntity<Void> {
        authorizeAdminOrKhadem(callerId, requireApprovePermission = true)
        userService.approveUser(userId, request)
        return ResponseEntity.ok().build()
    }

    @PatchMapping("/{userId}")
    fun updateMakhdoomProfile(
        @AuthenticationPrincipal callerId: UUID,
        @PathVariable userId: UUID,
        @Valid @RequestBody request: RegisterRequest
    ): ResponseEntity<Void> {
        authorizeAdminOrKhadem(callerId)
        userService.updateMakhdoomProfileByKhadem(callerId, userId, request)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/{userId}/code")
    fun updateCode(
        @AuthenticationPrincipal callerId: UUID,
        @PathVariable userId: UUID,
        @RequestBody request: UserCodeRequest
    ): ResponseEntity<Void> {
        userService.updateCode(callerId, userId, request.code)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/{userId}/reject")
    fun rejectUser(
        @AuthenticationPrincipal callerId: UUID, 
        @PathVariable userId: UUID,
        @RequestBody request: ActionReasonRequest
    ): ResponseEntity<Void> {
        authorizeAdminOrKhadem(callerId)
        userService.rejectUser(userId, request.reason)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/{userId}/ban")
    fun banUser(
        @AuthenticationPrincipal callerId: UUID, 
        @PathVariable userId: UUID,
        @RequestBody request: ActionReasonRequest
    ): ResponseEntity<Void> {
        authorizeAdminOrKhadem(callerId)
        userService.banUser(userId, request.reason)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/makhdoom", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createMakhdoom(
        @AuthenticationPrincipal callerId: UUID,
        @RequestPart("request") @Valid request: RegisterRequest,
        @RequestPart("image", required = false) image: MultipartFile?,
        @RequestPart("identityDocument", required = false) identityDocument: MultipartFile?
    ): ResponseEntity<ProfileResponse> {
        authorizeAdminOrKhadem(callerId)
        return ResponseEntity.ok(userService.createMakhdoomDirectly(callerId, request, image, identityDocument))
    }

    @PostMapping("/parent", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createParent(
        @AuthenticationPrincipal callerId: UUID,
        @RequestPart("request") @Valid request: RegisterRequest,
        @RequestPart("image", required = false) image: MultipartFile?,
        @RequestPart("nationalIdImage", required = false) nationalIdImage: MultipartFile?
    ): ResponseEntity<ProfileResponse> {
        authorizeAdminOrKhadem(callerId)
        return ResponseEntity.ok(userService.createParentDirectly(request, image, nationalIdImage))
    }

    @PutMapping("/{id}/parent-profile")
    fun updateParentProfile(
        @AuthenticationPrincipal callerId: UUID,
        @PathVariable id: UUID,
        @RequestBody request: ParentProfileRequest
    ): ResponseEntity<ProfileResponse> {
        authorizeAdminOrKhadem(callerId)
        return ResponseEntity.ok(userService.updateParentProfile(id, request))
    }
}
