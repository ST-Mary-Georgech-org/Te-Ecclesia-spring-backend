package org.teEcclesia.identity.api.controller

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.teEcclesia.identity.api.dto.request.UpdateAcademicYearRequest
import org.teEcclesia.identity.api.dto.response.AcademicYearResponse
import org.teEcclesia.identity.entity.enums.UserRole
import org.teEcclesia.identity.exception.UnauthorizedException
import org.teEcclesia.identity.service.SystemSettingService
import org.teEcclesia.identity.service.UserService
import java.util.UUID

@RestController
@RequestMapping("/api/v1/settings")
class SystemSettingController(
    private val systemSettingService: SystemSettingService,
    private val userService: UserService
) {

    @GetMapping("/academic-year")
    fun getCurrentAcademicYear(): ResponseEntity<AcademicYearResponse> {
        val currentYear = systemSettingService.getCurrentAcademicYear()
        return ResponseEntity.ok(AcademicYearResponse(currentYear))
    }

    @PutMapping("/academic-year")
    fun updateCurrentAcademicYear(
        @AuthenticationPrincipal callerId: UUID,
        @Valid @RequestBody request: UpdateAcademicYearRequest
    ): ResponseEntity<AcademicYearResponse> {
        val caller = userService.findProfileById(callerId)
        if (caller.role != UserRole.ADMIN) {
            throw UnauthorizedException("Only admins can change the academic year")
        }
        systemSettingService.updateCurrentAcademicYear(request.year)
        return ResponseEntity.ok(AcademicYearResponse(request.year))
    }
}
