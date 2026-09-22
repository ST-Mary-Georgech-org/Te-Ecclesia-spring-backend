package org.teEcclesia.identity.api.controller

import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID
import org.teEcclesia.identity.api.dto.request.*
import org.teEcclesia.identity.api.dto.response.LookupResponse
import org.teEcclesia.identity.entity.enums.UserRole
import org.teEcclesia.identity.exception.UnauthorizedException
import org.teEcclesia.identity.service.ManageLookupService
import org.teEcclesia.identity.service.UserService

@RestController
@RequestMapping("/api/v1/manage-lookups")
class ManageLookupController(
    private val manageLookupService: ManageLookupService,
    private val userService: UserService,
) {
    @PostMapping("/areas")
    fun createArea(
        @AuthenticationPrincipal callerId: UUID,
        @RequestBody request: AreaRequest,
    ): ResponseEntity<LookupResponse> {
        val caller = userService.findProfileById(callerId)
        if (caller.role != UserRole.ADMIN) {
            throw UnauthorizedException("Only admins can add new area")
        }
        return ResponseEntity.ok(manageLookupService.createArea(request))
    }

    @PutMapping("/areas/{id}")
    fun updateArea(
        @AuthenticationPrincipal callerId: UUID,
        @PathVariable id: Long,
        @RequestBody request: AreaRequest,
        ): ResponseEntity<LookupResponse> {
        val caller = userService.findProfileById(callerId)
        if (caller.role != UserRole.ADMIN) {
            throw UnauthorizedException("Only admins can update the area")
        }
        return ResponseEntity.ok(manageLookupService.updateArea(id, request))
    }

    @DeleteMapping("/areas/{id}")
    fun deleteArea(
        @AuthenticationPrincipal callerId: UUID,
        @PathVariable id: Long,
        ): ResponseEntity<Void> {
        val caller = userService.findProfileById(callerId)
        if (caller.role != UserRole.ADMIN) {
            throw UnauthorizedException("Only admins can delete the year")
        }
        manageLookupService.deleteArea(id)
        return ResponseEntity.noContent().build()
    }
}
