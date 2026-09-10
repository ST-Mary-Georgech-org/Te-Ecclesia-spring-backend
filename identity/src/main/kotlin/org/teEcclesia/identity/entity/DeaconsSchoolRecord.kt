package org.teEcclesia.identity.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import org.hibernate.envers.Audited
import org.teEcclesia.identity.entity.enums.DeaconsSchoolStatus
import java.math.BigDecimal

@Audited
@Entity
@Table(
    name = "deacons_school_records",
    schema = "identity",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_deacons_school_user_year", columnNames = ["user_id", "academic_year"])
    ]
)
data class DeaconsSchoolRecord(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(name = "academic_year", nullable = false)
    val academicYear: Int,

    @Column(name = "enrolled", nullable = false)
    val enrolled: Boolean,

    @Column(name = "paid", nullable = false)
    val paid: Boolean,

    @Column(name = "paid_amount", nullable = false, precision = 10, scale = 2)
    val paidAmount: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: DeaconsSchoolStatus
)
