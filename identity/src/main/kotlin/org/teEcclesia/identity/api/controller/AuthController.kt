package org.teEcclesia.identity.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.teEcclesia.identity.api.dto.request.*
import org.teEcclesia.identity.api.dto.response.AuthResponse
import org.teEcclesia.identity.api.dto.response.RegisterResponse
import org.teEcclesia.identity.api.dto.response.InitiateWhatsAppVerificationResponse
import org.teEcclesia.identity.api.dto.response.TokenResponse
import org.teEcclesia.identity.service.AuthService
import org.teEcclesia.identity.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.http.MediaType
import org.teEcclesia.identity.api.dto.response.ProfileResponse
import java.util.*

@RestController
@RequestMapping("/api/v1/identity/auth")
class AuthController (
    private val authService: AuthService,
    private val userService: UserService,
    @Value("\${storage.teEcclesia.cdn-endpoint}") cdnEndpoint: String,
    @Value("\${identity.resources.profile-image-directory}") profileImageDirectory: String
) {
    private val imagesBaseUrl: String = "$cdnEndpoint/$profileImageDirectory"

    @Tag(name = "Registration", description = "Endpoints related to user registration and account verification")
    @PostMapping("/signup", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun register(
        @RequestPart("request") @Valid request: RegisterRequest,
        @RequestPart("image", required = false) image: MultipartFile?,
        @RequestPart("certificateImage", required = false) certificateImage: MultipartFile?
    ): ResponseEntity<TokenResponse> {
        val response = authService.register(request, image, certificateImage)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @Tag(name = "Registration")
    @PostMapping("/complete-profile", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun completeProfile (
        @AuthenticationPrincipal userId: UUID,
        @RequestPart("request") @Valid request: CompleteProfileRequest,
        @RequestPart("certificateImage", required = false) certificateImage: MultipartFile?
    ): ResponseEntity<RegisterResponse> {
        val response = authService.completeProfile(userId, request, certificateImage)
        return ResponseEntity.ok(response)
    }

    @Tag(name = "Registration")
    @GetMapping("/me")
    fun getRegistrationProfile(
        @AuthenticationPrincipal userId: UUID
    ): ResponseEntity<ProfileResponse> {
        val response = userService.getUserProfile(userId, imagesBaseUrl)
        return ResponseEntity.ok(response)
    }

    @Tag(name = "Registration")
    @PostMapping("/verify-phone")
    fun verifyPhone(@Valid @RequestBody request: VerifyPhoneRequest): ResponseEntity<AuthResponse> {
        val response = authService.verifyPhone(request)
        return ResponseEntity.ok(response)
    }

    @Tag(name = "Registration")
    @PostMapping("/verify-email")
    fun verifyEmail(@Valid @RequestBody request: VerifyEmailRequest): ResponseEntity<AuthResponse> {
        val response = authService.verifyEmail(request)
        return ResponseEntity.ok(response)
    }

    @Tag(name = "Registration")
    @PostMapping("/whatsapp/initiate")
    fun initiateWhatsAppVerification(@RequestParam("phone") phone: String): ResponseEntity<InitiateWhatsAppVerificationResponse> {
        val response = authService.initiateWhatsAppVerification(phone)
        return ResponseEntity.ok(response)
    }

    @Tag(name = "Registration")
    @GetMapping("/whatsapp/status")
    fun getWhatsAppStatus(@RequestParam("token") token: String): ResponseEntity<AuthResponse> {
        val response = authService.getWhatsAppStatus(token)
        return ResponseEntity.ok(response)
    }



    @Tag(name = "Authentication", description = "Endpoints related to user login, token refresh, and logout")
    @PostMapping("/login")
    fun login (@Valid @RequestBody request: LoginRequest): ResponseEntity<AuthResponse> {
        val response = authService.login(request)
        return ResponseEntity.ok(response)
    }

    @Tag(name = "Authentication")
    @PostMapping("/refresh")
    fun refresh (@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<AuthResponse> {
        val response = authService.refreshToken(request)
        return ResponseEntity.ok(response)
    }

    @Tag(name = "Authentication")
    @PostMapping("/logout")
    fun logout(
        @AuthenticationPrincipal userId: UUID,
        @Valid @RequestBody request: RefreshTokenRequest
    ): ResponseEntity<Map<String, String>> {
        authService.logout(userId, request)
        return ResponseEntity.ok(mapOf("message" to "Logged out successfully"))
    }

    @Tag(name = "Authentication")
    @PatchMapping("/device-token")
    fun updateDeviceToken(
        @AuthenticationPrincipal userId: UUID,
        @Valid @RequestBody request: UpdateDeviceTokenRequest
    ): ResponseEntity<Unit> {
        authService.updateDeviceToken(userId, request.refreshToken, request.deviceToken)
        return ResponseEntity.ok().build()
    }

    @Tag(name = "Password Management", description = "Endpoints related to password reset and OTP verification")
    @PostMapping("/forgot-password")
    fun forgotPassword (@Valid @RequestBody request: ForgotPasswordRequest): ResponseEntity<String> {
        val response = authService.forgotPassword(request)
        return ResponseEntity.ok(response)
    }

    @Tag(name = "Password Management")
    @PostMapping("/verify-otp")
    fun verifyOtp(@Valid @RequestBody request: VerifyOtpRequest): ResponseEntity<String> {
        val response = authService.verifyOtp(request)
        return ResponseEntity.ok(response)
    }

    @Tag(name = "Password Management")
    @PostMapping("/reset-password")
    fun resetPassword (@Valid @RequestBody request: ResetPasswordRequest): ResponseEntity<String> {
        val response = authService.resetPassword(request)
        return ResponseEntity.ok(response)
    }

    @Operation(tags = ["Password Management", "Registration"], summary = "Resend OTP for account verification or password reset")
    @PostMapping("/resend-otp")
    fun resendOtp(@Valid @RequestBody request: ForgotPasswordRequest): ResponseEntity<String> {
        val response = authService.resendOtp(request)
        return ResponseEntity.ok(response)
    }
}