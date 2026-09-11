package org.teEcclesia.identity.attendance.service

import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.teEcclesia.identity.api.dto.response.LookupResponse
import org.teEcclesia.identity.attendance.dto.AddAttendeeRequest
import org.teEcclesia.identity.attendance.dto.AttendeeUserPreviewResponse
import org.teEcclesia.identity.attendance.dto.ChurchServiceResponse
import org.teEcclesia.identity.attendance.dto.CreateEventRequest
import org.teEcclesia.identity.attendance.dto.CreateServiceRequest
import org.teEcclesia.identity.attendance.dto.EventAttendeeResponse
import org.teEcclesia.identity.attendance.dto.ResponsibleServantDto
import org.teEcclesia.identity.attendance.dto.ServiceEventResponse
import org.teEcclesia.identity.attendance.entity.ChurchService
import org.teEcclesia.identity.attendance.entity.EventAttendee
import org.teEcclesia.identity.attendance.entity.ServiceEvent
import org.teEcclesia.identity.attendance.repository.ChurchServiceRepository
import org.teEcclesia.identity.attendance.repository.EventAttendeeRepository
import org.teEcclesia.identity.attendance.repository.ServiceEventRepository
import org.teEcclesia.identity.attendance.repository.projection.AttendeeCandidateProjection
import org.teEcclesia.identity.entity.enums.UserRole
import org.teEcclesia.identity.entity.lookups.EducationalStage
import org.teEcclesia.identity.exception.ResourceNotFoundException
import org.teEcclesia.identity.exception.UnauthorizedException
import org.teEcclesia.identity.repository.EducationalStageRepository
import org.teEcclesia.identity.repository.UserRepository
import java.util.UUID

@Service
class AttendanceService(
    private val churchServiceRepository: ChurchServiceRepository,
    private val serviceEventRepository: ServiceEventRepository,
    private val eventAttendeeRepository: EventAttendeeRepository,
    private val userRepository: UserRepository,
    private val educationalStageRepository: EducationalStageRepository
) {

    private fun checkAdmin(callerId: UUID) {
        val role = userRepository.findRoleById(callerId)
            ?: throw UnauthorizedException("User not found")
        if (role != UserRole.ADMIN) {
            throw UnauthorizedException("Only admins can manage services")
        }
    }

    private fun checkCanManageService(callerId: UUID, serviceId: Long): ChurchService {
        val role = userRepository.findRoleById(callerId)
            ?: throw UnauthorizedException("User not found")
        val service = churchServiceRepository.findByIdWithEducationalStages(serviceId)
            ?: throw ResourceNotFoundException("Service not found with id $serviceId")

        val isResponsible = role == UserRole.ADMIN || churchServiceRepository.isServantResponsibleForService(serviceId, callerId)
        if (!isResponsible) {
            throw UnauthorizedException("You are not authorized to manage events or attendees for this service")
        }
        return service
    }

    @Transactional(readOnly = true)
    fun getAllServices(callerId: UUID?, pageable: Pageable): Page<ChurchServiceResponse> {
        val callerRole = callerId?.let { userRepository.findRoleById(it) }
        val isCallerAdmin = callerRole == UserRole.ADMIN

        val servicesPage = churchServiceRepository.findAllWithEducationalStages(pageable)
        if (servicesPage.isEmpty) {
            return servicesPage.map { throw IllegalStateException() }
        }

        val serviceIds = servicesPage.content.map { it.id }
        val servantsByServiceId = churchServiceRepository.findResponsibleServantsByServiceIds(serviceIds)
            .groupBy { it.getServiceId() }

        val lang = LocaleContextHolder.getLocale().language
        val isEn = lang.startsWith("en", ignoreCase = true)

        return servicesPage.map { service ->
            val servants = servantsByServiceId[service.id]?.map { s ->
                ResponsibleServantDto(
                    id = s.getId(),
                    name = "${s.getFirstName()} ${s.getSecondName()} ${s.getThirdName()} ${s.getLastName()}".trim(),
                    code = s.getCode(),
                    imageUrl = s.getImageUrl()
                )
            } ?: emptyList()

            val isResponsible = isCallerAdmin || (callerId != null && servants.any { it.id == callerId })
            val stageResponses = service.educationalStages.map { stage ->
                val name = if (isEn) stage.nameEn else stage.nameAr
                LookupResponse(id = stage.id, name = name, whatsAppLink = null)
            }

            ChurchServiceResponse(
                id = service.id,
                name = service.name,
                createdAt = service.createdAt,
                responsible = isResponsible,
                educationalStages = stageResponses,
                responsibleServants = servants
            )
        }
    }

    @Transactional
    fun createService(creatorId: UUID, request: CreateServiceRequest): ChurchServiceResponse {
        checkAdmin(creatorId)

        val stages = resolveEducationalStages(request.educationalStageIds)
        val servantIds = resolveResponsibleServantIds(request.responsibleServantIds)

        val service = churchServiceRepository.save(
            ChurchService(
                name = request.name,
                educationalStages = stages,
                responsibleServantIds = servantIds,
                createdById = creatorId
            )
        )
        return getSingleServiceResponse(service.id, creatorId, isCallerAdmin = true)
    }

    @Transactional
    fun updateService(callerId: UUID, id: Long, request: CreateServiceRequest): ChurchServiceResponse {
        checkAdmin(callerId)

        val service = churchServiceRepository.findByIdOrNull(id)
            ?: throw ResourceNotFoundException("Service not found with id $id")

        val stages = resolveEducationalStages(request.educationalStageIds)
        val servantIds = resolveResponsibleServantIds(request.responsibleServantIds)

        val updated = churchServiceRepository.save(
            service.copy(
                name = request.name,
                educationalStages = stages,
                responsibleServantIds = servantIds
            )
        )
        return getSingleServiceResponse(updated.id, callerId, isCallerAdmin = true)
    }

    private fun resolveEducationalStages(incomingStageIds: List<Long>?): MutableSet<EducationalStage> {
        val stageIds = incomingStageIds ?: emptyList()
        if (stageIds.size > 10) {
            throw IllegalArgumentException("Cannot assign more than 10 educational stages to a service")
        }
        if (stageIds.isEmpty()) return mutableSetOf()
        val foundStages = educationalStageRepository.findAllById(stageIds)
        if (foundStages.size != stageIds.size) {
            throw ResourceNotFoundException("One or more educational stages not found")
        }
        return foundStages.toMutableSet()
    }

    private fun resolveResponsibleServantIds(incomingServantIds: List<UUID>?): MutableSet<UUID> {
        val servantIds = incomingServantIds ?: emptyList()
        if (servantIds.size > 30) {
            throw IllegalArgumentException("Cannot assign more than 30 responsible servants to a service")
        }
        if (servantIds.isEmpty()) return mutableSetOf()
        val validIds = userRepository.findApprovedKhademIdsByIds(servantIds).toSet()
        if (validIds.size != servantIds.size) {
            throw IllegalArgumentException("One or more selected servants are invalid or not approved khadems")
        }
        return servantIds.toMutableSet()
    }

    private fun getSingleServiceResponse(serviceId: Long, callerId: UUID, isCallerAdmin: Boolean): ChurchServiceResponse {
        val service = churchServiceRepository.findByIdWithEducationalStages(serviceId)
            ?: throw ResourceNotFoundException("Service not found with id $serviceId")

        val servants = churchServiceRepository.findResponsibleServantsByServiceIds(listOf(serviceId)).map { s ->
            ResponsibleServantDto(
                id = s.getId(),
                name = "${s.getFirstName()} ${s.getSecondName()} ${s.getThirdName()} ${s.getLastName()}".trim(),
                code = s.getCode(),
                imageUrl = s.getImageUrl()
            )
        }

        val lang = LocaleContextHolder.getLocale().language
        val isEn = lang.startsWith("en", ignoreCase = true)
        val stageResponses = service.educationalStages.map { stage ->
            val name = if (isEn) stage.nameEn else stage.nameAr
            LookupResponse(id = stage.id, name = name, whatsAppLink = null)
        }

        val isResponsible = isCallerAdmin || servants.any { it.id == callerId }

        return ChurchServiceResponse(
            id = service.id,
            name = service.name,
            createdAt = service.createdAt,
            responsible = isResponsible,
            educationalStages = stageResponses,
            responsibleServants = servants
        )
    }

    @Transactional
    fun deleteService(callerId: UUID, id: Long) {
        checkAdmin(callerId)
        if (!churchServiceRepository.existsById(id)) {
            throw ResourceNotFoundException("Service not found with id $id")
        }
        churchServiceRepository.deleteById(id)
    }

    @Transactional(readOnly = true)
    fun getEventsByServiceId(serviceId: Long, pageable: Pageable): Page<ServiceEventResponse> {
        return serviceEventRepository.findAllByServiceIdOrderByEventDateDescStartTimeDesc(serviceId, pageable).map {
            ServiceEventResponse(it.id, it.serviceId, it.name, it.eventDate, it.startTime, it.endTime, it.createdAt)
        }
    }

    @Transactional
    fun createEvent(creatorId: UUID, serviceId: Long, request: CreateEventRequest): ServiceEventResponse {
        val service = checkCanManageService(creatorId, serviceId)

        val event = serviceEventRepository.save(
            ServiceEvent(
                serviceId = service.id,
                name = request.name?.ifBlank { null },
                eventDate = request.eventDate,
                startTime = request.startTime,
                endTime = request.endTime,
                createdById = creatorId
            )
        )
        return ServiceEventResponse(event.id, event.serviceId, event.name, event.eventDate, event.startTime, event.endTime, event.createdAt)
    }

    @Transactional
    fun updateEvent(callerId: UUID, eventId: Long, request: CreateEventRequest): ServiceEventResponse {
        val event = serviceEventRepository.findByIdOrNull(eventId)
            ?: throw ResourceNotFoundException("Event not found with id $eventId")

        checkCanManageService(callerId, event.serviceId)

        val updated = serviceEventRepository.save(
            event.copy(
                name = request.name?.ifBlank { null },
                eventDate = request.eventDate,
                startTime = request.startTime,
                endTime = request.endTime
            )
        )
        return ServiceEventResponse(updated.id, updated.serviceId, updated.name, updated.eventDate, updated.startTime, updated.endTime, updated.createdAt)
    }

    @Transactional
    fun deleteEvent(callerId: UUID, eventId: Long) {
        val event = serviceEventRepository.findByIdOrNull(eventId)
            ?: throw ResourceNotFoundException("Event not found with id $eventId")

        checkCanManageService(callerId, event.serviceId)
        serviceEventRepository.deleteById(eventId)
    }

    @Transactional(readOnly = true)
    fun getAttendeesByEventId(eventId: Long, pageable: Pageable): Page<EventAttendeeResponse> {
        val lang = LocaleContextHolder.getLocale().language
        val isEn = lang.startsWith("en", ignoreCase = true)
        return eventAttendeeRepository.findAttendeesByEventId(eventId, pageable).map { p ->
            val stageName = if (isEn) {
                p.getMakhdoomStageEn() ?: p.getKhademStageEn()
            } else {
                p.getMakhdoomStageAr() ?: p.getKhademStageAr()
            }
            val yearName = if (isEn) {
                p.getMakhdoomYearEn() ?: p.getKhademYearEn()
            } else {
                p.getMakhdoomYearAr() ?: p.getKhademYearAr()
            }
            val fullName = "${p.getFirstName()} ${p.getSecondName()} ${p.getThirdName()} ${p.getLastName()}"

            EventAttendeeResponse(
                id = p.getId(),
                eventId = p.getEventId(),
                userId = p.getUserId().toString(),
                name = fullName,
                role = p.getRole(),
                stageName = stageName,
                yearName = yearName,
                registeredAt = p.getRegisteredAt()
            )
        }
    }

    @Transactional(readOnly = true)
    fun searchUsersForAttendance(query: String): List<AttendeeUserPreviewResponse> {
        val raw = query.trim()
        if (raw.length < 2) return emptyList()

        val normalized = normalizeArabic(raw)
        val digits = extractNumericCode(raw)
        val candidates = userRepository.searchAttendanceCandidates(
            normalizedQuery = normalized,
            rawQuery = raw,
            numericQuery = digits,
            pageable = PageRequest.of(0, 10)
        )
        return mapCandidatesToPreview(candidates)
    }

    @Transactional(readOnly = true)
    fun searchServants(query: String): List<AttendeeUserPreviewResponse> {
        val raw = query.trim()
        if (raw.length < 2) return emptyList()

        val normalized = normalizeArabic(raw)
        val digits = extractNumericCode(raw)
        val candidates = userRepository.searchServantCandidates(
            normalizedQuery = normalized,
            rawQuery = raw,
            numericQuery = digits,
            pageable = PageRequest.of(0, 10)
        )
        return mapCandidatesToPreview(candidates)
    }

    private fun mapCandidatesToPreview(candidates: List<AttendeeCandidateProjection>): List<AttendeeUserPreviewResponse> {
        val lang = LocaleContextHolder.getLocale().language
        val isEn = lang.startsWith("en", ignoreCase = true)
        return candidates.map { c ->
            val stageName = if (isEn) {
                c.getMakhdoomStageEn() ?: c.getKhademStageEn()
            } else {
                c.getMakhdoomStageAr() ?: c.getKhademStageAr()
            }
            val yearName = if (isEn) {
                c.getMakhdoomYearEn() ?: c.getKhademYearEn()
            } else {
                c.getMakhdoomYearAr() ?: c.getKhademYearAr()
            }
            val fullName = "${c.getFirstName()} ${c.getSecondName()} ${c.getThirdName()} ${c.getLastName()}"

            AttendeeUserPreviewResponse(
                id = c.getId().toString(),
                name = fullName,
                role = c.getRole(),
                stageName = stageName,
                yearName = yearName,
                code = c.getCode(),
                imageUrl = c.getImageUrl()
            )
        }
    }

    private fun normalizeArabic(input: String): String {
        return input.lowercase().trim()
            .replace(Regex("[أإآ]"), "ا")
            .replace('ة', 'ه')
            .replace('ى', 'ي')
            .replace('ؤ', 'و')
            .replace('ئ', 'ء')
            .replace(Regex("[\\u064B-\\u065F\\u0670]"), "")
    }

    private fun extractNumericCode(input: String): String {
        val digits = input.filter { it.isDigit() }
        return if (digits.length >= 6) digits else ""
    }

    private fun findCandidateByCodeOrNumericCode(code: String): AttendeeCandidateProjection? {
        val trimmed = code.trim()
        val digits = extractNumericCode(trimmed)
        return userRepository.findCandidateByCode(trimmed, digits).firstOrNull()
    }

    @Transactional(readOnly = true)
    fun getUserByCode(code: String): AttendeeUserPreviewResponse {
        val candidate = findCandidateByCodeOrNumericCode(code)
            ?: throw ResourceNotFoundException("User not found with code $code")
        val lang = LocaleContextHolder.getLocale().language
        val isEn = lang.startsWith("en", ignoreCase = true)
        val stageName = if (isEn) {
            candidate.getMakhdoomStageEn() ?: candidate.getKhademStageEn()
        } else {
            candidate.getMakhdoomStageAr() ?: candidate.getKhademStageAr()
        }
        val yearName = if (isEn) {
            candidate.getMakhdoomYearEn() ?: candidate.getKhademYearEn()
        } else {
            candidate.getMakhdoomYearAr() ?: candidate.getKhademYearAr()
        }
        val fullName = "${candidate.getFirstName()} ${candidate.getSecondName()} ${candidate.getThirdName()} ${candidate.getLastName()}".trim()
        return AttendeeUserPreviewResponse(
            id = candidate.getId().toString(),
            name = fullName,
            role = candidate.getRole(),
            stageName = stageName,
            yearName = yearName,
            code = candidate.getCode(),
            imageUrl = candidate.getImageUrl()
        )
    }

    @Transactional
    fun addAttendee(callerId: UUID, eventId: Long, request: AddAttendeeRequest): EventAttendeeResponse {
        val event = serviceEventRepository.findByIdOrNull(eventId)
            ?: throw ResourceNotFoundException("Event not found with id $eventId")

        val service = checkCanManageService(callerId, event.serviceId)

        val candidate = findCandidateByCodeOrNumericCode(request.code)
            ?: throw ResourceNotFoundException("User not found with code ${request.code}")

        if (service.educationalStages.isNotEmpty()) {
            val candidateStageId = candidate.getMakhdoomStageId() ?: candidate.getKhademStageId()
            val allowedStageIds = service.educationalStages.map { it.id }.toSet()
            if (candidateStageId == null || !allowedStageIds.contains(candidateStageId)) {
                val isEn = LocaleContextHolder.getLocale().language.startsWith("en", ignoreCase = true)
                val stageTitles = service.educationalStages.joinToString(", ") { if (isEn) it.nameEn else it.nameAr }
                val message = if (isEn) {
                    "This user does not belong to any educational stage of this service ($stageTitles)"
                } else {
                    "هذا الشخص لا ينتمي لأي من المراحل الدراسية المحددة لهذه الخدمة ($stageTitles)"
                }
                throw IllegalArgumentException(message)
            }
        }

        val lang = LocaleContextHolder.getLocale().language
        val isEn = lang.startsWith("en", ignoreCase = true)
        val stageName = if (isEn) {
            candidate.getMakhdoomStageEn() ?: candidate.getKhademStageEn()
        } else {
            candidate.getMakhdoomStageAr() ?: candidate.getKhademStageAr()
        }
        val yearName = if (isEn) {
            candidate.getMakhdoomYearEn() ?: candidate.getKhademYearEn()
        } else {
            candidate.getMakhdoomYearAr() ?: candidate.getKhademYearAr()
        }
        val fullName = "${candidate.getFirstName()} ${candidate.getSecondName()} ${candidate.getThirdName()} ${candidate.getLastName()}".trim()

        val existing = eventAttendeeRepository.findByEventIdAndUserId(eventId, candidate.getId())
        if (existing != null) {
            return EventAttendeeResponse(
                id = existing.id,
                eventId = existing.eventId,
                userId = candidate.getId().toString(),
                name = fullName,
                role = candidate.getRole(),
                stageName = stageName,
                yearName = yearName,
                registeredAt = existing.registeredAt
            )
        }

        val attendee = eventAttendeeRepository.save(
            EventAttendee(
                eventId = eventId,
                userId = candidate.getId()
            )
        )
        return EventAttendeeResponse(
            id = attendee.id,
            eventId = attendee.eventId,
            userId = candidate.getId().toString(),
            name = fullName,
            role = candidate.getRole(),
            stageName = stageName,
            yearName = yearName,
            registeredAt = attendee.registeredAt
        )
    }

    @Transactional
    fun removeAttendee(callerId: UUID, eventId: Long, userId: UUID) {
        val event = serviceEventRepository.findByIdOrNull(eventId)
            ?: throw ResourceNotFoundException("Event not found with id $eventId")

        checkCanManageService(callerId, event.serviceId)
        eventAttendeeRepository.deleteByEventIdAndUserId(eventId, userId)
    }
}


