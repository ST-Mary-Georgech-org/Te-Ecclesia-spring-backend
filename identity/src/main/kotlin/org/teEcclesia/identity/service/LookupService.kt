package org.teEcclesia.identity.service

import org.springframework.stereotype.Service
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.teEcclesia.identity.api.dto.request.*
import org.teEcclesia.identity.api.dto.response.LookupResponse
import org.teEcclesia.identity.entity.lookups.*
import org.teEcclesia.identity.repository.*
import org.teEcclesia.identity.exception.ResourceNotFoundException

@Service
class LookupService(
    private val rankRepository: RankRepository,
    private val educationalStageRepository: EducationalStageRepository,
    private val educationalYearRepository: EducationalYearRepository,
    private val areaRepository: AreaRepository
) {
    fun getRanks(lang: String, pageable: Pageable): Page<LookupResponse> = rankRepository.findAll(pageable).map {
        LookupResponse(it.id, if (lang.startsWith("en")) it.nameEn else it.nameAr)
    }

    fun getEducationalStages(lang: String, pageable: Pageable): Page<LookupResponse> = educationalStageRepository.findAll(pageable).map { stage ->
        LookupResponse(
            id = stage.id, 
            name = if (lang.startsWith("en")) stage.nameEn else stage.nameAr,
            subItems = stage.years.map { year ->
                LookupResponse(
                    id = year.id,
                    name = if (lang.startsWith("en")) year.nameEn else year.nameAr
                )
            }
        )
    }

    fun getAreas(pageable: Pageable): Page<LookupResponse> = areaRepository.findAll(pageable).map {
        LookupResponse(it.id, it.name)
    }

    fun createRank(request: RankRequest): LookupResponse {
        val rank = Rank(
            nameAr = request.nameAr,
            nameEn = request.nameEn,
            codeLetter = request.codeLetter,
        )
        val saved = rankRepository.save(rank)
        return LookupResponse(saved.id, saved.nameAr)
    }

    fun updateRank(id: Long, request: RankRequest): LookupResponse {
        val rank = rankRepository.findById(id).orElseThrow { ResourceNotFoundException("Rank not found") }
        val updated = rankRepository.save(rank.copy(
            nameAr = request.nameAr,
            nameEn = request.nameEn,
            codeLetter = request.codeLetter,
        ))
        return LookupResponse(updated.id, updated.nameAr)
    }

    fun deleteRank(id: Long) {
        rankRepository.deleteById(id)
    }

    fun createEducationalStage(request: EducationalStageRequest): LookupResponse {
        val stage = EducationalStage(
            nameAr = request.nameAr,
            nameEn = request.nameEn
        )
        val saved = educationalStageRepository.save(stage)
        return LookupResponse(saved.id, saved.nameAr)
    }

    fun updateEducationalStage(id: Long, request: EducationalStageRequest): LookupResponse {
        val stage = educationalStageRepository.findById(id).orElseThrow { ResourceNotFoundException("Stage not found") }
        val updated = educationalStageRepository.save(stage.copy(
            nameAr = request.nameAr,
            nameEn = request.nameEn
        ))
        return LookupResponse(updated.id, updated.nameAr)
    }

    fun deleteEducationalStage(id: Long) {
        educationalStageRepository.deleteById(id)
    }

    fun createEducationalYear(request: EducationalYearRequest): LookupResponse {
        val stage = educationalStageRepository.findById(request.stageId)
            .orElseThrow { ResourceNotFoundException("Stage not found") }
        val year = EducationalYear(
            nameAr = request.nameAr,
            nameEn = request.nameEn,
            stage = stage
        )
        val saved = educationalYearRepository.save(year)
        return LookupResponse(saved.id, saved.nameAr)
    }

    fun updateEducationalYear(id: Long, request: EducationalYearRequest): LookupResponse {
        val year = educationalYearRepository.findById(id).orElseThrow { ResourceNotFoundException("Year not found") }
        val stage = educationalStageRepository.findById(request.stageId)
            .orElseThrow { ResourceNotFoundException("Stage not found") }
        val updated = educationalYearRepository.save(year.copy(
            nameAr = request.nameAr,
            nameEn = request.nameEn,
            stage = stage
        ))
        return LookupResponse(updated.id, updated.nameAr)
    }

    fun deleteEducationalYear(id: Long) {
        educationalYearRepository.deleteById(id)
    }

    fun createArea(request: AreaRequest): LookupResponse {
        val area = Area(name = request.name)
        val saved = areaRepository.save(area)
        return LookupResponse(saved.id, saved.name)
    }

    fun updateArea(id: Long, request: AreaRequest): LookupResponse {
        val area = areaRepository.findById(id).orElseThrow { ResourceNotFoundException("Area not found") }
        val updated = areaRepository.save(area.copy(name = request.name))
        return LookupResponse(updated.id, updated.name)
    }

    fun deleteArea(id: Long) {
        areaRepository.deleteById(id)
    }
}
