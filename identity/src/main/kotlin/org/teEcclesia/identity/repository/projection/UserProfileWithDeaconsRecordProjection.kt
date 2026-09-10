package org.teEcclesia.identity.repository.projection

import org.teEcclesia.identity.entity.DeaconsSchoolRecord
import org.teEcclesia.identity.entity.User

interface UserProfileWithDeaconsRecordProjection {
    fun getUser(): User
    fun getDeaconsSchoolRecord(): DeaconsSchoolRecord?
}
