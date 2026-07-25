package org.teEcclesia.events.identity

import org.teEcclesia.events.TeEcclesiaEvent
import java.util.UUID

data class UserLoggedInEvent(val userId: UUID) : TeEcclesiaEvent
