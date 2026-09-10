package org.teEcclesia.identity.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.teEcclesia.identity.entity.SystemSetting
import org.teEcclesia.identity.entity.enums.SettingKey
import org.teEcclesia.identity.repository.SystemSettingRepository

@Service
class SystemSettingService(
    private val systemSettingRepository: SystemSettingRepository
) {
    @Transactional(readOnly = true)
    fun getSettingValue(key: SettingKey): String? {
        return systemSettingRepository.findById(key).map { it.value }.orElse(null)
    }

    @Transactional
    fun updateSettingValue(key: SettingKey, value: String?, description: String? = null): SystemSetting {
        val setting = systemSettingRepository.findById(key)
            .map { it.copy(value = value, description = description ?: it.description) }
            .orElseGet { SystemSetting(key = key, value = value, description = description) }
        return systemSettingRepository.save(setting)
    }

    @Transactional(readOnly = true)
    fun getCurrentAcademicYear(): Int {
        val value = getSettingValue(SettingKey.CURRENT_ACADEMIC_YEAR)
        return value?.toIntOrNull() ?: 2026
    }

    @Transactional
    fun updateCurrentAcademicYear(year: Int): SystemSetting {
        require(year in 2000..2100) { "Academic year must be between 2000 and 2100" }
        return updateSettingValue(SettingKey.CURRENT_ACADEMIC_YEAR, year.toString(), "Current Academic Year")
    }
}
