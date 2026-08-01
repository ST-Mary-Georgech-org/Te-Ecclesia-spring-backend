package org.teEcclesia.identity.api.controller

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID
import org.teEcclesia.identity.api.dto.request.*
import org.teEcclesia.identity.api.dto.response.LookupResponse
import org.teEcclesia.identity.entity.enums.UserRole
import org.teEcclesia.identity.service.LookupService

@RestController
@RequestMapping("/api/v1/lookups")
class LookupController(
    private val lookupService: LookupService
) {

    @GetMapping("/ranks")
    fun getRanks(
        @RequestHeader(value = "Accept-Language", defaultValue = "ar") language: String,
        pageable: Pageable
    ): ResponseEntity<Page<LookupResponse>> {
        return ResponseEntity.ok(lookupService.getRanks(language, pageable))
    }

    @GetMapping("/educational-stages")
    fun getEducationalStages(
        @RequestHeader(value = "Accept-Language", defaultValue = "ar") language: String,
        @AuthenticationPrincipal userId: UUID?,
        @RequestParam(value = "forRole", required = false) forRole: UserRole?,
        pageable: Pageable
    ): ResponseEntity<Page<LookupResponse>> {
        return ResponseEntity.ok(lookupService.getEducationalStages(language, userId, forRole, pageable))
    }

    @GetMapping("/areas")
    fun getAreas(
        @RequestParam(value = "query", required = false) query: String?,
        pageable: Pageable
    ): ResponseEntity<Page<LookupResponse>> {
        return ResponseEntity.ok(lookupService.getAreas(query, pageable))
    }

    @PostMapping("/ranks")
    fun createRank(@RequestBody request: RankRequest): ResponseEntity<LookupResponse> {
        return ResponseEntity.ok(lookupService.createRank(request))
    }

    @PutMapping("/ranks/{id}")
    fun updateRank(@PathVariable id: Long, @RequestBody request: RankRequest): ResponseEntity<LookupResponse> {
        return ResponseEntity.ok(lookupService.updateRank(id, request))
    }

    @DeleteMapping("/ranks/{id}")
    fun deleteRank(@PathVariable id: Long): ResponseEntity<Void> {
        lookupService.deleteRank(id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/educational-stages")
    fun createEducationalStage(@RequestBody request: EducationalStageRequest): ResponseEntity<LookupResponse> {
        return ResponseEntity.ok(lookupService.createEducationalStage(request))
    }

    @PutMapping("/educational-stages/{id}")
    fun updateEducationalStage(@PathVariable id: Long, @RequestBody request: EducationalStageRequest): ResponseEntity<LookupResponse> {
        return ResponseEntity.ok(lookupService.updateEducationalStage(id, request))
    }

    @DeleteMapping("/educational-stages/{id}")
    fun deleteEducationalStage(@PathVariable id: Long): ResponseEntity<Void> {
        lookupService.deleteEducationalStage(id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/educational-years")
    fun createEducationalYear(@RequestBody request: EducationalYearRequest): ResponseEntity<LookupResponse> {
        return ResponseEntity.ok(lookupService.createEducationalYear(request))
    }

    @PutMapping("/educational-years/{id}")
    fun updateEducationalYear(@PathVariable id: Long, @RequestBody request: EducationalYearRequest): ResponseEntity<LookupResponse> {
        return ResponseEntity.ok(lookupService.updateEducationalYear(id, request))
    }

    @DeleteMapping("/educational-years/{id}")
    fun deleteEducationalYear(@PathVariable id: Long): ResponseEntity<Void> {
        lookupService.deleteEducationalYear(id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/areas")
    fun createArea(@RequestBody request: AreaRequest): ResponseEntity<LookupResponse> {
        return ResponseEntity.ok(lookupService.createArea(request))
    }

    @PutMapping("/areas/{id}")
    fun updateArea(@PathVariable id: Long, @RequestBody request: AreaRequest): ResponseEntity<LookupResponse> {
        return ResponseEntity.ok(lookupService.updateArea(id, request))
    }

    @DeleteMapping("/areas/{id}")
    fun deleteArea(@PathVariable id: Long): ResponseEntity<Void> {
        lookupService.deleteArea(id)
        return ResponseEntity.noContent().build()
    }
}
