package org.teEcclesia.identity.service

import org.springframework.stereotype.Service
import org.teEcclesia.identity.api.dto.request.*
import org.teEcclesia.identity.api.dto.response.LookupResponse
import org.teEcclesia.identity.entity.lookups.*
import org.teEcclesia.identity.exception.ResourceNotFoundException
import org.teEcclesia.identity.repository.*
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ManageLookupService(
    private val areaRepository: AreaRepository,
) {
    @Transactional
    fun createArea(request: AreaRequest): LookupResponse {
        val area = Area(name = request.name)
        val saved = areaRepository.save(area)
        return LookupResponse(saved.id, saved.name, whatsAppLink = null)
    }

    @Transactional
    fun updateArea(id: Long, request: AreaRequest): LookupResponse {
        val area = areaRepository.findById(id).orElseThrow { ResourceNotFoundException("Area not found") }
        val updated = areaRepository.save(area.copy(name = request.name))
        return LookupResponse(updated.id, updated.name, whatsAppLink = null)
    }

    @Transactional
    fun deleteArea(id: Long) {
        areaRepository.deleteById(id)
    }
}
