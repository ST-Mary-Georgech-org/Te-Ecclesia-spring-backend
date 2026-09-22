package org.teEcclesia.identity.attendance.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.envers.Audited
import java.time.Instant
import java.util.UUID

@Audited
@Entity
@Table(
    name = "event_attendees",
    schema = "identity",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_event_attendee_event_user", columnNames = ["event_id", "user_id"])
    ],
    indexes = [
        Index(name = "idx_event_attendees_event_id", columnList = "event_id")
    ]
)
data class EventAttendee(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "event_id", nullable = false)
    val eventId: Long,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "registered_at", nullable = false)
    val registeredAt: Instant = Instant.now()
)

