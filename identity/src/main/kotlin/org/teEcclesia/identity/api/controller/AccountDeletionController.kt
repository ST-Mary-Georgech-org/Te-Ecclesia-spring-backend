package org.teEcclesia.identity.api.controller

import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.teEcclesia.identity.api.dto.request.DeleteAccountRequest
import org.teEcclesia.identity.api.dto.request.ReactivateAccountRequest
import org.teEcclesia.identity.api.dto.response.AccountDeletionRequestResponse
import org.teEcclesia.identity.api.dto.response.AuthResponse
import org.teEcclesia.identity.api.dto.response.DeletionRequestCountResponse
import org.teEcclesia.identity.service.AccountDeletionService
import java.util.UUID

@RestController
@RequestMapping("/api/v1/identity")
class AccountDeletionController(
    private val accountDeletionService: AccountDeletionService
) {

    @Tag(name = "Account Deletion")
    @PostMapping("/account/delete")
    fun requestAccountDeletion(
        @AuthenticationPrincipal userId: UUID,
        @Valid @RequestBody request: DeleteAccountRequest
    ): ResponseEntity<Unit> {
        accountDeletionService.requestAccountDeletion(userId, request)
        return ResponseEntity.ok().build()
    }

    @Tag(name = "Account Deletion")
    @PostMapping("/auth/reactivate")
    fun reactivateAccount(
        @Valid @RequestBody request: ReactivateAccountRequest
    ): ResponseEntity<AuthResponse> {
        val response = accountDeletionService.reactivateAccount(request)
        return ResponseEntity.ok(response)
    }

    @Tag(name = "Admin Account Deletion")
    @GetMapping("/admin/deletion-requests")
    fun getDeletionRequests(
        @AuthenticationPrincipal adminId: UUID,
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<Page<AccountDeletionRequestResponse>> {
        val response = accountDeletionService.getDeletionRequests(adminId, pageable)
        return ResponseEntity.ok(response)
    }

    @Tag(name = "Admin Account Deletion")
    @GetMapping("/admin/deletion-requests/count")
    fun getDeletionRequestCount(
        @AuthenticationPrincipal adminId: UUID
    ): ResponseEntity<DeletionRequestCountResponse> {
        val count = accountDeletionService.getDeletionRequestCount(adminId)
        return ResponseEntity.ok(DeletionRequestCountResponse(count))
    }

    @Tag(name = "Admin Account Deletion")
    @PostMapping("/admin/deletion-requests/{id}/approve")
    fun approveDeletion(
        @AuthenticationPrincipal adminId: UUID,
        @PathVariable("id") id: UUID
    ): ResponseEntity<Unit> {
        accountDeletionService.approveDeletion(adminId, id)
        return ResponseEntity.ok().build()
    }

    @Tag(name = "Admin Account Deletion")
    @PostMapping("/admin/deletion-requests/{id}/reject")
    fun rejectDeletion(
        @AuthenticationPrincipal adminId: UUID,
        @PathVariable("id") id: UUID
    ): ResponseEntity<Unit> {
        accountDeletionService.rejectDeletion(adminId, id)
        return ResponseEntity.ok().build()
    }
}
