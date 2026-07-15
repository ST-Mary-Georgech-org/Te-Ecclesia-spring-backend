package org.teEcclesia.identity.service

import org.springframework.stereotype.Service
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.teEcclesia.identity.api.dto.response.LookupResponse
import org.teEcclesia.identity.repository.*

@Service
class LookupService(
    private val rankRepository: RankRepository,
    private val educationalStageRepository: EducationalStageRepository,
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
}
