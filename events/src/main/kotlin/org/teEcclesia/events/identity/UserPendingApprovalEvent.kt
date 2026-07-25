package org.teEcclesia.events.identity

import org.teEcclesia.events.TeEcclesiaEvent
import java.util.UUID

data class UserPendingApprovalEvent(
    val userId: UUID,
    val userName: String
) : TeEcclesiaEvent
