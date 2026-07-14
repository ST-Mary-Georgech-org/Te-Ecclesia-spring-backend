package org.teEcclesia.events.publisher

import org.teEcclesia.events.TeEcclesiaEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
internal class TeEcclesiaEventPublisherImp(
    private val applicationEventPublisher: ApplicationEventPublisher
) : TeEcclesiaEventPublisher {

    override fun publish(event: TeEcclesiaEvent) {
        applicationEventPublisher.publishEvent(event)
    }
}