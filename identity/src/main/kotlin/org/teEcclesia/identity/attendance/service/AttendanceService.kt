package org.teEcclesia.identity.attendance.service

import org.springframework.context.i18n.LocaleContextHolder
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
    fun getAttendeesByEventId(eventId: Long): List<EventAttendeeResponse> {
        return eventAttendeeRepository.findAllByEventIdOrderByRegisteredAtDesc(eventId).map {
            mapToEventAttendeeResponse(it)
        }
    }

    @Transactional(readOnly = true)
    fun getUserByCode(code: String): AttendeeUserPreviewResponse {
        val user = userRepository.findByCode(code)
            ?: throw ResourceNotFoundException("User not found with code $code")
        return mapToAttendeeUserPreview(user)
    }

    @Transactional
    fun addAttendee(eventId: Long, request: AddAttendeeRequest): EventAttendeeResponse {
        if (!serviceEventRepository.existsById(eventId)) {
            throw ResourceNotFoundException("Event not found with id $eventId")
        }
        val user = userRepository.findByCode(request.code)
            ?: throw ResourceNotFoundException("User not found with code ${request.code}")

        if (eventAttendeeRepository.existsByEventIdAndUserId(eventId, user.id)) {
            val existing = eventAttendeeRepository.findAllByEventIdOrderByRegisteredAtDesc(eventId)
                .first { it.user.id == user.id }
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
            name = user.displayName,
            role = user.role,
            stageName = stageName,
            yearName = yearName,
            code = user.code
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
