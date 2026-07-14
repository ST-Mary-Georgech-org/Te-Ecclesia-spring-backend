package org.teEcclesia.events.identity

import org.teEcclesia.events.TeEcclesiaEvent
import java.util.*

data class UserCreatedEvent(
    val id: UUID,
    val password: String,
    val fullName: String,
    val imageUrl: String?
) : TeEcclesiaEvent