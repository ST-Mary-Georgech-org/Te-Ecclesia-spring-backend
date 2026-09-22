package org.teEcclesia.identity.attendance.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.ConstraintMode
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.envers.Audited
import org.hibernate.envers.NotAudited
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Audited
@Entity
@Table(name = "repeated_events", schema = "identity")
data class RepeatedEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "service_id", nullable = false)
    val serviceId: Long,

    @NotAudited
    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "service_id",
        insertable = false,
        updatable = false,
        foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    val churchService: ChurchService? = null,

    @Column(nullable = true)
    val name: String?,

    @Column(name = "event_start_date", nullable = false)
    val startDate: LocalDate,

    @Column(name = "next_creational_date", nullable = false)
    val nextCreationDate: LocalDate,

    @Column(name = "start_time", nullable = false)
    val startTime: LocalTime,

    @Column(name = "end_time", nullable = false)
    val endTime: LocalTime,

    @Column(name = "created_by_id", nullable = false)
    val createdById: UUID,

    @Column(name = "repeatEvery", nullable = true)
    val repeatEvery: Int,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RepeatedEvent) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "RepeatedEvent(id=$id, serviceId=$serviceId, name=$name)"
    }
}

