package org.teEcclesia.audit

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.core.annotation.AuthenticationPrincipal
import java.util.UUID
import org.teEcclesia.identity.service.UserService
import org.teEcclesia.identity.entity.enums.UserRole
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/v1/audit")
class AuditController(
    private val auditService: AuditService,
    private val userService: UserService
) {

    private fun verifyAdmin(userId: UUID) {
        val user = userService.findById(userId)
        if (user.role != UserRole.ADMIN) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Only admin can access audit endpoints")
        }
    }

    @GetMapping("/entities")
    fun getAuditableEntities(
        @AuthenticationPrincipal userId: UUID
    ): ResponseEntity<List<AuditableEntityDto>> {
        verifyAdmin(userId)
        return ResponseEntity.ok(auditService.getAuditableEntities())
    }

    @GetMapping
    fun getAudits(
        @AuthenticationPrincipal userId: UUID,
        @RequestParam entityType: String,
        @RequestParam(required = false) entityId: String?,
        pageable: Pageable
    ): ResponseEntity<Page<AuditDto>> {
        verifyAdmin(userId)
        val audits = auditService.getAudits(entityType, entityId, pageable)
        return ResponseEntity.ok(audits)
    }

    @PostMapping("/revert")
    fun revertEntity(
        @AuthenticationPrincipal userId: UUID,
        @RequestParam entityType: String,
        @RequestParam entityId: String,
        @RequestParam revisionId: Int
    ): ResponseEntity<String> {
        verifyAdmin(userId)
        auditService.revertToRevision(entityType, entityId, revisionId)
        return ResponseEntity.ok("Entity reverted successfully")
    }
}
