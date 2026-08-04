package org.teEcclesia.identity.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.teEcclesia.identity.entity.SystemSetting
import org.teEcclesia.identity.entity.enums.SettingKey

interface SystemSettingRepository : JpaRepository<SystemSetting, SettingKey>
