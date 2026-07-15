package org.teEcclesia.identity.api.controller

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.teEcclesia.identity.api.dto.request.RegisterRequest
import org.teEcclesia.identity.api.dto.response.ProfileResponse
import org.teEcclesia.identity.entity.enums.UserStatus
import org.teEcclesia.identity.service.UserService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.teEcclesia.identity.entity.enums.UserRole
import org.teEcclesia.identity.exception.UnauthorizedException
import java.util.UUID

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService
) {

    private fun authorizeAdminOrKhadem(callerId: UUID) {
        val caller = userService.findById(callerId)
        if (caller.role != UserRole.ADMIN && caller.role != UserRole.KHADEM) {
            throw UnauthorizedException("User is not authorized for this action")
        }
    }

    @GetMapping("/status/{status}")
    fun getUsersByStatus(
        @AuthenticationPrincipal callerId: UUID,
        @PathVariable status: UserStatus,
        pageable: Pageable
    ): ResponseEntity<Page<ProfileResponse>> {
        authorizeAdminOrKhadem(callerId)
        return ResponseEntity.ok(userService.getUsersByStatus(status, pageable))
    }

    @PostMapping("/{userId}/approve")
    fun approveUser(@AuthenticationPrincipal callerId: UUID, @PathVariable userId: UUID): ResponseEntity<Void> {
        authorizeAdminOrKhadem(callerId)
        userService.approveUser(userId)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/{userId}/reject")
    fun rejectUser(@AuthenticationPrincipal callerId: UUID, @PathVariable userId: UUID): ResponseEntity<Void> {
        authorizeAdminOrKhadem(callerId)
        userService.rejectUser(userId)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/{userId}/ban")
    fun banUser(@AuthenticationPrincipal callerId: UUID, @PathVariable userId: UUID): ResponseEntity<Void> {
        authorizeAdminOrKhadem(callerId)
        userService.banUser(userId)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/makhdoom")
    fun createMakhdoom(
        @AuthenticationPrincipal callerId: UUID,
        @RequestBody request: RegisterRequest
    ): ResponseEntity<ProfileResponse> {
        authorizeAdminOrKhadem(callerId)
        return ResponseEntity.ok(userService.createMakhdoomDirectly(request))
    }
}
