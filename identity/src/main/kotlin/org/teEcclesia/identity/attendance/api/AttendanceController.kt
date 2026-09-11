package org.teEcclesia.identity.attendance.api

import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.teEcclesia.identity.attendance.dto.AddAttendeeRequest
import org.teEcclesia.identity.attendance.dto.AttendeeUserPreviewResponse
import org.teEcclesia.identity.attendance.dto.ChurchServiceResponse
import org.teEcclesia.identity.attendance.dto.CreateEventRequest
import org.teEcclesia.identity.attendance.dto.CreateServiceRequest
import org.teEcclesia.identity.attendance.dto.EventAttendeeResponse
import org.teEcclesia.identity.attendance.dto.ServiceEventResponse
import org.teEcclesia.identity.attendance.service.AttendanceService
import java.util.UUID

@RestController
@RequestMapping("/api/v1/attendance")
class AttendanceController(
    private val attendanceService: AttendanceService
) {

    @GetMapping("/services")
    fun getAllServices(
        @AuthenticationPrincipal callerId: UUID?,
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<Page<ChurchServiceResponse>> {
        return ResponseEntity.ok(attendanceService.getAllServices(callerId, pageable))
    }

    @PostMapping("/services")
    fun createService(
        @AuthenticationPrincipal callerId: UUID,
        @Valid @RequestBody request: CreateServiceRequest
    ): ResponseEntity<ChurchServiceResponse> {
        return ResponseEntity.ok(attendanceService.createService(callerId, request))
    }

    @PutMapping("/services/{id}")
    fun updateService(
        @AuthenticationPrincipal callerId: UUID,
        @PathVariable id: Long,
        @Valid @RequestBody request: CreateServiceRequest
    ): ResponseEntity<ChurchServiceResponse> {
        return ResponseEntity.ok(attendanceService.updateService(callerId, id, request))
    }

    @DeleteMapping("/services/{id}")
    fun deleteService(
        @AuthenticationPrincipal callerId: UUID,
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        attendanceService.deleteService(callerId, id)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/services/{serviceId}/events")
    fun getEventsByServiceId(
        @PathVariable serviceId: Long,
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<Page<ServiceEventResponse>> {
        return ResponseEntity.ok(attendanceService.getEventsByServiceId(serviceId, pageable))
    }

    @PostMapping("/services/{serviceId}/events")
    fun createEvent(
        @AuthenticationPrincipal callerId: UUID,
        @PathVariable serviceId: Long,
        @RequestBody request: CreateEventRequest
    ): ResponseEntity<ServiceEventResponse> {
        return ResponseEntity.ok(attendanceService.createEvent(callerId, serviceId, request))
    }

    @PutMapping("/events/{eventId}")
    fun updateEvent(
        @AuthenticationPrincipal callerId: UUID,
        @PathVariable eventId: Long,
        @RequestBody request: CreateEventRequest
    ): ResponseEntity<ServiceEventResponse> {
        return ResponseEntity.ok(attendanceService.updateEvent(callerId, eventId, request))
    }

    @DeleteMapping("/events/{eventId}")
    fun deleteEvent(
        @AuthenticationPrincipal callerId: UUID,
        @PathVariable eventId: Long
    ): ResponseEntity<Void> {
        attendanceService.deleteEvent(callerId, eventId)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/events/{eventId}/attendees")
    fun getAttendeesByEventId(
        @PathVariable eventId: Long,
        pageable: Pageable
    ): ResponseEntity<Page<EventAttendeeResponse>> {
        return ResponseEntity.ok(attendanceService.getAttendeesByEventId(eventId, pageable))
    }

    @GetMapping("/users/search")
    fun searchUsers(
        @RequestParam query: String
    ): ResponseEntity<List<AttendeeUserPreviewResponse>> {
        return ResponseEntity.ok(attendanceService.searchUsersForAttendance(query))
    }

    @GetMapping("/servants/search")
    fun searchServants(
        @RequestParam query: String
    ): ResponseEntity<List<AttendeeUserPreviewResponse>> {
        return ResponseEntity.ok(attendanceService.searchServants(query))
    }

    @GetMapping("/users/by-code/{code}")
    fun getUserByCode(@PathVariable code: String): ResponseEntity<AttendeeUserPreviewResponse> {
        return ResponseEntity.ok(attendanceService.getUserByCode(code))
    }

    @PostMapping("/events/{eventId}/attendees")
    fun addAttendee(
        @AuthenticationPrincipal callerId: UUID,
        @PathVariable eventId: Long,
        @Valid @RequestBody request: AddAttendeeRequest
    ): ResponseEntity<EventAttendeeResponse> {
        return ResponseEntity.ok(attendanceService.addAttendee(callerId, eventId, request))
    }

    @DeleteMapping("/events/{eventId}/attendees/{userId}")
    fun removeAttendee(
        @AuthenticationPrincipal callerId: UUID,
        @PathVariable eventId: Long,
        @PathVariable userId: UUID
    ): ResponseEntity<Void> {
        attendanceService.removeAttendee(callerId, eventId, userId)
        return ResponseEntity.ok().build()
    }
}
