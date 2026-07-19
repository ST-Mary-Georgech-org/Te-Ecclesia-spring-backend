package org.teEcclesia.identity.api.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.teEcclesia.identity.api.dto.request.VerifyTokenRequest
import org.teEcclesia.identity.api.dto.response.VerifyTokenResponse
import org.teEcclesia.identity.service.AuthService

@RestController
@RequestMapping("/api/v1/internal/whatsapp")
class InternalAuthController(
    private val authService: AuthService
) {

    @PostMapping("/verify-token")
    fun verifyToken(@RequestBody request: VerifyTokenRequest): ResponseEntity<VerifyTokenResponse> {
        val response = authService.processWhatsAppVerification(request.token, request.fromNumber)
        return ResponseEntity.ok(response)
    }
}
