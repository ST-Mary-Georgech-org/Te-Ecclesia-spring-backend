package org.teEcclesia.identity.attendance.api

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
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
    fun getAllServices(): ResponseEntity<List<ChurchServiceResponse>> {
        return ResponseEntity.ok(attendanceService.getAllServices())
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
        @PathVariable id: Long,
        @Valid @RequestBody request: CreateServiceRequest
    ): ResponseEntity<ChurchServiceResponse> {
        return ResponseEntity.ok(attendanceService.updateService(id, request))
    }

    @DeleteMapping("/services/{id}")
    fun deleteService(@PathVariable id: Long): ResponseEntity<Void> {
        attendanceService.deleteService(id)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/services/{serviceId}/events")
    fun getEventsByServiceId(@PathVariable serviceId: Long): ResponseEntity<List<ServiceEventResponse>> {
        return ResponseEntity.ok(attendanceService.getEventsByServiceId(serviceId))
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
        @PathVariable eventId: Long,
        @RequestBody request: CreateEventRequest
    ): ResponseEntity<ServiceEventResponse> {
        return ResponseEntity.ok(attendanceService.updateEvent(eventId, request))
    }

    @DeleteMapping("/events/{eventId}")
    fun deleteEvent(@PathVariable eventId: Long): ResponseEntity<Void> {
        attendanceService.deleteEvent(eventId)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/events/{eventId}/attendees")
    fun getAttendeesByEventId(@PathVariable eventId: Long): ResponseEntity<List<EventAttendeeResponse>> {
        return ResponseEntity.ok(attendanceService.getAttendeesByEventId(eventId))
    }

    @GetMapping("/users/by-code/{code}")
    fun getUserByCode(@PathVariable code: String): ResponseEntity<AttendeeUserPreviewResponse> {
        return ResponseEntity.ok(attendanceService.getUserByCode(code))
    }

    @PostMapping("/events/{eventId}/attendees")
    fun addAttendee(
        @PathVariable eventId: Long,
        @Valid @RequestBody request: AddAttendeeRequest
    ): ResponseEntity<EventAttendeeResponse> {
        return ResponseEntity.ok(attendanceService.addAttendee(eventId, request))
    }

    @DeleteMapping("/events/{eventId}/attendees/{userId}")
    fun removeAttendee(
        @PathVariable eventId: Long,
        @PathVariable userId: UUID
    ): ResponseEntity<Void> {
        attendanceService.removeAttendee(eventId, userId)
        return ResponseEntity.ok().build()
    }
}
