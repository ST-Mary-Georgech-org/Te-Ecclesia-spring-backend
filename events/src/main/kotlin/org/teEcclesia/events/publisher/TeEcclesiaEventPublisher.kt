package org.teEcclesia.events.publisher

import org.teEcclesia.events.TeEcclesiaEvent

interface TeEcclesiaEventPublisher {
    fun publish(event: TeEcclesiaEvent)
}