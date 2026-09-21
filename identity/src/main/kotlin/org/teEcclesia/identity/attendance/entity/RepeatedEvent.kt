package org.teEcclesia.identity.attendance.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Entity
@Table(name = "repeated_events", schema = "identity")
data class RepeatedEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "service_id", nullable = false)
    val serviceId: Long,

    @Column(nullable = true)
    val name: String?,

    @Column(name = "event_start_date", nullable = false)
    val startDate: LocalDate,

    @Column(name = "next_creational_date", nullable = false)
    var nextCreationDate: LocalDate,

    @Column(name = "start_time", nullable = false)
    val startTime: LocalTime,

    @Column(name = "end_time", nullable = false)
    val endTime: LocalTime,

    @Column(name = "created_by_id", nullable = false)
    val createdById: UUID,

    @Column(name = "repeatEvery", nullable = true)
    val repeatEvery: Int = 1,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)

