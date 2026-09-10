package org.teEcclesia.identity.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.teEcclesia.identity.entity.DeaconsSchoolRecord
import java.util.UUID

interface DeaconsSchoolRecordRepository : JpaRepository<DeaconsSchoolRecord, Long> {
    fun findByUserIdAndAcademicYear(userId: UUID, academicYear: Int): DeaconsSchoolRecord?
    fun deleteByUserIdAndAcademicYear(userId: UUID, academicYear: Int)
}
