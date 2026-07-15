package org.teEcclesia.identity.api.controller

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.teEcclesia.identity.api.dto.response.LookupResponse
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
        pageable: Pageable
    ): ResponseEntity<Page<LookupResponse>> {
        return ResponseEntity.ok(lookupService.getEducationalStages(language, pageable))
    }

    @GetMapping("/areas")
    fun getAreas(
        pageable: Pageable
    ): ResponseEntity<Page<LookupResponse>> {
        return ResponseEntity.ok(lookupService.getAreas(pageable))
    }
}
