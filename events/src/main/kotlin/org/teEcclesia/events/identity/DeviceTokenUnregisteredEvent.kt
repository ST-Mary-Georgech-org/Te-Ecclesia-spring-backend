package org.teEcclesia.events.identity

import org.teEcclesia.events.TeEcclesiaEvent

data class DeviceTokenUnregisteredEvent(
    val token: String
) : TeEcclesiaEvent
