package org.teEcclesia.identity.api.controller

import jakarta.validation.Valid
import org.teEcclesia.identity.api.dto.request.UpdateProfileRequest
import org.teEcclesia.identity.api.dto.response.InitiateWhatsAppVerificationResponse
import org.teEcclesia.identity.api.dto.response.ProfileResponse
import org.teEcclesia.identity.api.dto.response.UpdateImageResponse
import org.teEcclesia.identity.service.UserService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*

@RestController
@RequestMapping("/api/v1/identity/profile")
class ProfileController(
    private val userService: UserService,
    @Value("\${storage.teEcclesia.cdn-endpoint}") cdnEndpoint: String,
    @Value("\${identity.resources.profile-image-directory}") profileImageDirectory: String,
) {
    private val imagesBaseUrl: String = "$cdnEndpoint/$profileImageDirectory"

    @GetMapping
    fun getUserProfile(
        @AuthenticationPrincipal userId: UUID
    ): ResponseEntity<ProfileResponse> {
        val response = userService.getUserProfile(userId, imagesBaseUrl)
        return ResponseEntity.ok(response)
    }

    @PatchMapping(path = ["/image"], consumes = ["multipart/form-data"])
    fun updateUserImage(
        @AuthenticationPrincipal userId: UUID,
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<UpdateImageResponse> {
        val imageUri = userService.updateUserImage(userId, file)
        val response = UpdateImageResponse(imageUrl = "$imagesBaseUrl/$imageUri")
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/image")
    fun deleteUserImage(@AuthenticationPrincipal userId: UUID): ResponseEntity<Unit> {
        userService.deleteUserImage(userId)
        return ResponseEntity.ok().build()
    }

    @PatchMapping
    fun updateProfile(
        @AuthenticationPrincipal userId: UUID,
        @Valid @RequestBody request: UpdateProfileRequest
    ): ResponseEntity<Unit> {
        userService.updateProfile(userId, request)
        return ResponseEntity.ok().build()
    }

    @PatchMapping("/phone")
    fun initiatePhoneChange(
        @AuthenticationPrincipal userId: UUID,
        @RequestParam("phone") phone: String
    ): ResponseEntity<InitiateWhatsAppVerificationResponse> {
        val response = userService.initiatePhoneChange(userId, phone)
        return ResponseEntity.ok(response)
    }
}