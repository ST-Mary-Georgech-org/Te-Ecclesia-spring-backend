package org.teEcclesia.notifications.entity

enum class NotificationType {
    ALERT,
    SYSTEM,
    REVIEW;

    companion object {
        fun fromStringOrDefault(type: String): NotificationType {
            return entries.firstOrNull { it.name.equals(type, ignoreCase = true) } ?: SYSTEM
        }
    }
}