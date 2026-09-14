package org.teEcclesia.identity.api.controller

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.teEcclesia.identity.api.dto.request.AdminSendNotificationRequest
import org.teEcclesia.identity.api.dto.response.AdminSendNotificationResponse
import org.teEcclesia.identity.service.AdminNotificationService
import java.util.UUID

@RestController
@RequestMapping("/api/v1/admin/notifications")
class AdminNotificationController(
    private val adminNotificationService: AdminNotificationService
) {
    @PostMapping("/send")
    fun sendNotification(
        @AuthenticationPrincipal callerId: UUID,
        @Valid @RequestBody request: AdminSendNotificationRequest
    ): ResponseEntity<AdminSendNotificationResponse> {
        val response = adminNotificationService.sendNotification(callerId, request)
        return ResponseEntity.ok(response)
    }
}
