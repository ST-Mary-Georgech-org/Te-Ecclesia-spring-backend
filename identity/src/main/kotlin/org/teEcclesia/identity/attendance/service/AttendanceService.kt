package org.teEcclesia.identity.attendance.service

import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.teEcclesia.identity.attendance.dto.AddAttendeeRequest
import org.teEcclesia.identity.attendance.dto.AttendeeUserPreviewResponse
import org.teEcclesia.identity.attendance.dto.ChurchServiceResponse
import org.teEcclesia.identity.attendance.dto.CreateEventRequest
import org.teEcclesia.identity.attendance.dto.CreateServiceRequest
import org.teEcclesia.identity.attendance.dto.EventAttendeeResponse
import org.teEcclesia.identity.attendance.dto.ServiceEventResponse
import org.teEcclesia.identity.attendance.entity.ChurchService
import org.teEcclesia.identity.attendance.entity.EventAttendee
import org.teEcclesia.identity.attendance.entity.ServiceEvent
import org.teEcclesia.identity.attendance.repository.ChurchServiceRepository
import org.teEcclesia.identity.attendance.repository.EventAttendeeRepository
import org.teEcclesia.identity.attendance.repository.ServiceEventRepository
import org.teEcclesia.identity.entity.User
import org.teEcclesia.identity.exception.ResourceNotFoundException
import org.teEcclesia.identity.repository.UserRepository
import java.util.UUID

@Service
class AttendanceService(
    private val churchServiceRepository: ChurchServiceRepository,
    private val serviceEventRepository: ServiceEventRepository,
    private val eventAttendeeRepository: EventAttendeeRepository,
    private val userRepository: UserRepository
) {

    @Transactional(readOnly = true)
    fun getAllServices(): List<ChurchServiceResponse> {
        return churchServiceRepository.findAllByOrderByCreatedAtDesc().map {
            ChurchServiceResponse(it.id, it.name, it.createdAt)
        }
    }

    @Transactional
    fun createService(creatorId: UUID, request: CreateServiceRequest): ChurchServiceResponse {
        val user = userRepository.getReferenceById(creatorId)
        val service = churchServiceRepository.save(
            ChurchService(
                name = request.name,
                user = user
            )
        )
        return ChurchServiceResponse(service.id, service.name, service.createdAt)
    }

    @Transactional
    fun updateService(id: Long, request: CreateServiceRequest): ChurchServiceResponse {
        val service = churchServiceRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Service not found with id $id") }
        val updated = churchServiceRepository.save(service.copy(name = request.name))
        return ChurchServiceResponse(updated.id, updated.name, updated.createdAt)
    }

    @Transactional
    fun deleteService(id: Long) {
        if (!churchServiceRepository.existsById(id)) {
            throw ResourceNotFoundException("Service not found with id $id")
        }
        churchServiceRepository.deleteById(id)
    }

    @Transactional(readOnly = true)
    fun getEventsByServiceId(serviceId: Long): List<ServiceEventResponse> {
        return serviceEventRepository.findAllByServiceIdOrderByEventDateDescStartTimeDesc(serviceId).map {
            ServiceEventResponse(it.id, it.serviceId, it.name, it.eventDate, it.startTime, it.endTime, it.createdAt)
        }
    }

    @Transactional
    fun createEvent(creatorId: UUID, serviceId: Long, request: CreateEventRequest): ServiceEventResponse {
        if (!churchServiceRepository.existsById(serviceId)) {
            throw ResourceNotFoundException("Service not found with id $serviceId")
        }
        val user = userRepository.getReferenceById(creatorId)
        val event = serviceEventRepository.save(
            ServiceEvent(
                serviceId = serviceId,
                name = request.name?.ifBlank { null },
                eventDate = request.eventDate,
                startTime = request.startTime,
                endTime = request.endTime,
                user = user
            )
        )
        return ServiceEventResponse(event.id, event.serviceId, event.name, event.eventDate, event.startTime, event.endTime, event.createdAt)
    }

    @Transactional
    fun updateEvent(eventId: Long, request: CreateEventRequest): ServiceEventResponse {
        val event = serviceEventRepository.findById(eventId)
            .orElseThrow { ResourceNotFoundException("Event not found with id $eventId") }
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
    fun deleteEvent(eventId: Long) {
        if (!serviceEventRepository.existsById(eventId)) {
            throw ResourceNotFoundException("Event not found with id $eventId")
        }
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

    private fun findUserByCodeOrNumericCode(code: String): User? {
        val trimmed = code.trim()
        val digits = extractNumericCode(trimmed)
        return userRepository.findByCodeIgnoringPrefixLetter(trimmed, digits).firstOrNull()
            ?: userRepository.findByCode(trimmed)
    }

    @Transactional(readOnly = true)
    fun getUserByCode(code: String): AttendeeUserPreviewResponse {
        val user = findUserByCodeOrNumericCode(code)
            ?: throw ResourceNotFoundException("User not found with code $code")
        return mapToAttendeeUserPreview(user)
    }

    @Transactional
    fun addAttendee(eventId: Long, request: AddAttendeeRequest): EventAttendeeResponse {
        if (!serviceEventRepository.existsById(eventId)) {
            throw ResourceNotFoundException("Event not found with id $eventId")
        }
        val user = findUserByCodeOrNumericCode(request.code)
            ?: throw ResourceNotFoundException("User not found with code ${request.code}")

        val existing = eventAttendeeRepository.findByEventIdAndUserId(eventId, user.id)
        if (existing != null) {
            return mapToEventAttendeeResponse(existing)
        }

        val attendee = eventAttendeeRepository.save(
            EventAttendee(
                eventId = eventId,
                user = user
            )
        )
        return mapToEventAttendeeResponse(attendee)
    }

    @Transactional
    fun removeAttendee(eventId: Long, userId: UUID) {
        eventAttendeeRepository.deleteByEventIdAndUserId(eventId, userId)
    }

    private fun mapToAttendeeUserPreview(user: User): AttendeeUserPreviewResponse {
        val lang = LocaleContextHolder.getLocale().language
        val stageName = user.makhdoomProfile?.educationalStage?.let {
            if (lang.startsWith("en", ignoreCase = true)) it.nameEn else it.nameAr
        } ?: user.khademProfile?.educationalStage?.let {
            if (lang.startsWith("en", ignoreCase = true)) it.nameEn else it.nameAr
        }
        val yearName = user.makhdoomProfile?.educationalYear?.let {
            if (lang.startsWith("en", ignoreCase = true)) it.nameEn else it.nameAr
        } ?: user.khademProfile?.educationalYear?.let {
            if (lang.startsWith("en", ignoreCase = true)) it.nameEn else it.nameAr
        }

        return AttendeeUserPreviewResponse(
            id = user.id.toString(),
            name = user.fullName,
            role = user.role,
            stageName = stageName,
            yearName = yearName,
            code = user.code,
            imageUrl = user.imageUrl
        )
    }

    private fun mapToEventAttendeeResponse(attendee: EventAttendee): EventAttendeeResponse {
        val preview = mapToAttendeeUserPreview(attendee.user)
        return EventAttendeeResponse(
            id = attendee.id,
            eventId = attendee.eventId,
            userId = preview.id,
            name = preview.name,
            role = preview.role,
            stageName = preview.stageName,
            yearName = preview.yearName,
            registeredAt = attendee.registeredAt
        )
    }
}
