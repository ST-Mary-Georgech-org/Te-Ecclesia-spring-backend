package org.teEcclesia.identity.api.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.teEcclesia.identity.api.dto.request.PendingTokenRequest
import org.teEcclesia.identity.api.dto.request.VerifyTokenRequest
import org.teEcclesia.identity.api.dto.response.GetPendingTokenResponse
import org.teEcclesia.identity.api.dto.response.SavePendingTokenResponse
import org.teEcclesia.identity.api.dto.response.VerifyTokenResponse
import org.teEcclesia.identity.service.AuthService

@RestController
@RequestMapping("/api/v1/internal/whatsapp")
class InternalAuthController(
    private val authService: AuthService
) {

    @PostMapping("/pending-token")
    fun handlePendingToken(@RequestBody request: PendingTokenRequest): ResponseEntity<Any> {
        return if (request.action.equals("get", ignoreCase = true)) {
            val token = authService.getPendingToken(request.userId)
            ResponseEntity.ok(GetPendingTokenResponse(token = token))
        } else {
            val token = request.token ?: throw IllegalArgumentException("Token is required to save pending token")
            authService.savePendingToken(request.userId, token)
            ResponseEntity.ok(SavePendingTokenResponse(status = "saved"))
        }
    }

    @PostMapping("/verify-token")
    fun verifyToken(@RequestBody request: VerifyTokenRequest): ResponseEntity<VerifyTokenResponse> {
        val response = authService.processWhatsAppVerification(
            tokenStr = request.token,
            fromNumber = request.fromNumber,
            userId = request.userId
        )
        return ResponseEntity.ok(response)
    }
}
