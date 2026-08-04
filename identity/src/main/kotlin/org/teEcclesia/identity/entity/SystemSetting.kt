package org.teEcclesia.identity.entity

import jakarta.persistence.*
import org.hibernate.envers.Audited
import org.teEcclesia.identity.entity.enums.SettingKey

@Audited
@Entity
@Table(name = "system_settings", schema = "identity")
data class SystemSetting(
    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "setting_key", nullable = false, unique = true)
    val key: SettingKey,

    @Column(name = "setting_value", nullable = true)
    val value: String? = null,

    @Column(name = "description", nullable = true)
    val description: String? = null
)
