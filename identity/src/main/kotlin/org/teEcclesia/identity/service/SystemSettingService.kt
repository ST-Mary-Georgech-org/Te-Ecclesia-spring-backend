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
}
