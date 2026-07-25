package org.teEcclesia.notifications.eventListener

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.teEcclesia.client.ApiClient
import org.teEcclesia.events.notifications.EmailEvent
import org.springframework.context.event.EventListener
import org.springframework.http.HttpMethod
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class AsyncEmailListener(
    private val apiClient: ApiClient,
    @Value("\${email.service.url}") private val emailServiceUrl: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @EventListener
    fun handleEmailEvent(event: EmailEvent) {
        try {
            val requestBody = mapOf(
                "email" to event.to,
                "subject" to event.subject,
                "message" to event.text
            )
            apiClient.call(String::class.java) {
                includeHMAC = true
                path = emailServiceUrl.trimEnd('/')
                method = HttpMethod.POST
                body = requestBody
                addToken = false
            }

            log.info("Email sent successfully to ${event.to} with subject '${event.subject}'.")
        } catch (e: Exception) {
            log.error("Failed to send email to ${event.to}", e)
        }
    }
}