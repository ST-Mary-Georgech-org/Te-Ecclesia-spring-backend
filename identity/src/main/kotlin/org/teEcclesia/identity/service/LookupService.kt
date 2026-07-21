package org.teEcclesia.identity.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.teEcclesia.identity.api.dto.request.*
import org.teEcclesia.identity.api.dto.response.LookupResponse
import org.teEcclesia.identity.entity.KhademProfile
import org.teEcclesia.identity.entity.enums.UserRole
import org.teEcclesia.identity.entity.lookups.*
import org.teEcclesia.identity.exception.ResourceNotFoundException
import org.teEcclesia.identity.repository.*
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional(readOnly = true)
class LookupService(
    private val rankRepository: RankRepository,
    private val educationalStageRepository: EducationalStageRepository,
    private val educationalYearRepository: EducationalYearRepository,
    private val areaRepository: AreaRepository,
    private val userRepository: UserRepository
) {
    fun getRanks(lang: String, pageable: Pageable): Page<LookupResponse> = rankRepository.findAll(pageable).map {
        LookupResponse(it.id, if (lang.startsWith("en")) it.nameEn else it.nameAr)
    }

    fun getEducationalStages(lang: String, userId: UUID?, pageable: Pageable): Page<LookupResponse> {
        val user = userId?.let { userRepository.findById(it).orElse(null) }
        return if (user?.role == UserRole.KHADEM && user.khademProfile != null) {
            fetchEducationalStagesForKhadem(user.khademProfile, lang, pageable)
        } else {
            fetchEducationalStages(pageable, lang)
        }
    }

    private fun fetchEducationalStages(
        pageable: Pageable,
        lang: String
    ): Page<LookupResponse> {
        val allStages = educationalStageRepository.findAll(pageable)

        return allStages.map { stage ->
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
    }

    private fun fetchEducationalStagesForKhadem(
        khademProfile: KhademProfile,
        lang: String,
        pageable: Pageable
    ): Page<LookupResponse> {
        val responsibleStagesIds = khademProfile.responsibleStages.map { it.id }.toSet()
        val responsibleYearsIds = khademProfile.responsibleYears.map { it.id }.toSet()
        val educationalStages = educationalStageRepository.findByIdIn(responsibleStagesIds)
        val educationalStagesByYears = educationalStageRepository.findByYearsIdIn(responsibleYearsIds)
        val stagesAfterRemoveUnreasonableYearsFromIt = educationalStagesByYears.map { stage ->
            val filteredYears = stage.years.filter { responsibleYearsIds.contains(it.id) }.toMutableList()
            stage.copy(years = filteredYears)
        }

        val filteredStages = (educationalStages + stagesAfterRemoveUnreasonableYearsFromIt).distinctBy { it.id }

        val result = filteredStages.map { stage ->
            val isFullStage = responsibleStagesIds.contains(stage.id)
            LookupResponse(
                id = stage.id,
                name = if (lang.startsWith("en")) stage.nameEn else stage.nameAr,
                subItems = stage.years
                    .filter { isFullStage || responsibleYearsIds.contains(it.id) }
                    .map { year ->
                        LookupResponse(
                            id = year.id,
                            name = if (lang.startsWith("en")) year.nameEn else year.nameAr
                        )
                    }
            )
        }
        return PageImpl(result, pageable, result.size.toLong())
    }

    fun getAreas(query: String?, pageable: Pageable): Page<LookupResponse> =
        areaRepository.searchAreas(query?.trim(), pageable).map {
            LookupResponse(it.id, it.name)
        }

    @Transactional
    fun createRank(request: RankRequest): LookupResponse {
        val rank = Rank(
            nameAr = request.nameAr,
            nameEn = request.nameEn,
            codeLetter = request.codeLetter,
        )
        val saved = rankRepository.save(rank)
        return LookupResponse(saved.id, saved.nameAr)
    }

    @Transactional
    fun updateRank(id: Long, request: RankRequest): LookupResponse {
        val rank = rankRepository.findById(id).orElseThrow { ResourceNotFoundException("Rank not found") }
        val updated = rankRepository.save(
            rank.copy(
                nameAr = request.nameAr,
                nameEn = request.nameEn,
                codeLetter = request.codeLetter,
            )
        )
        return LookupResponse(updated.id, updated.nameAr)
    }

    @Transactional
    fun deleteRank(id: Long) {
        rankRepository.deleteById(id)
    }

    @Transactional
    fun createEducationalStage(request: EducationalStageRequest): LookupResponse {
        val stage = EducationalStage(
            nameAr = request.nameAr,
            nameEn = request.nameEn
        )
        val saved = educationalStageRepository.save(stage)
        return LookupResponse(saved.id, saved.nameAr)
    }

    @Transactional
    fun updateEducationalStage(id: Long, request: EducationalStageRequest): LookupResponse {
        val stage = educationalStageRepository.findById(id).orElseThrow { ResourceNotFoundException("Stage not found") }
        val updated = educationalStageRepository.save(
            stage.copy(
                nameAr = request.nameAr,
                nameEn = request.nameEn
            )
        )
        return LookupResponse(updated.id, updated.nameAr)
    }

    @Transactional
    fun deleteEducationalStage(id: Long) {
        educationalStageRepository.deleteById(id)
    }

    @Transactional
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

    @Transactional
    fun updateEducationalYear(id: Long, request: EducationalYearRequest): LookupResponse {
        val year = educationalYearRepository.findById(id).orElseThrow { ResourceNotFoundException("Year not found") }
        val stage = educationalStageRepository.findById(request.stageId)
            .orElseThrow { ResourceNotFoundException("Stage not found") }
        val updated = educationalYearRepository.save(
            year.copy(
                nameAr = request.nameAr,
                nameEn = request.nameEn,
                stage = stage
            )
        )
        return LookupResponse(updated.id, updated.nameAr)
    }

    @Transactional
    fun deleteEducationalYear(id: Long) {
        educationalYearRepository.deleteById(id)
    }

    @Transactional
    fun createArea(request: AreaRequest): LookupResponse {
        val area = Area(name = request.name)
        val saved = areaRepository.save(area)
        return LookupResponse(saved.id, saved.name)
    }

    @Transactional
    fun updateArea(id: Long, request: AreaRequest): LookupResponse {
        val area = areaRepository.findById(id).orElseThrow { ResourceNotFoundException("Area not found") }
        val updated = areaRepository.save(area.copy(name = request.name))
        return LookupResponse(updated.id, updated.name)
    }

    @Transactional
    fun deleteArea(id: Long) {
        areaRepository.deleteById(id)
    }
}
