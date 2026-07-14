package org.teEcclesia.events.notifications

import org.teEcclesia.events.TeEcclesiaEvent

data class EmailEvent(
    val to: String,
    val subject: String,
    val text: String,
) : TeEcclesiaEvent