package org.teEcclesia.identity.attendance.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.teEcclesia.identity.entity.User
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Entity
@Table(name = "service_events")
data class ServiceEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "service_id", nullable = false)
    val serviceId: Long,

    @Column(nullable = true)
    val name: String? = null,

    @Column(name = "event_date", nullable = false)
    val eventDate: LocalDate,

    @Column(name = "start_time", nullable = false)
    val startTime: LocalTime,

    @Column(name = "end_time", nullable = false)
    val endTime: LocalTime,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    val user: User,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
