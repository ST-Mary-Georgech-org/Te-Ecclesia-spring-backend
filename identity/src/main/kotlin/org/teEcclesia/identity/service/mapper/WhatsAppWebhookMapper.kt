package org.teEcclesia.identity.service.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.teEcclesia.identity.api.dto.request.WhatsAppWebhookMessage

@Component
class WhatsAppWebhookMapper(private val objectMapper: ObjectMapper) {

    fun parse(requestBody: String): WhatsAppWebhookMessage? {
        val payloadMap = runCatching {
            objectMapper.readValue(requestBody, Map::class.java)
        }.getOrElse { return null }

        val entryList = payloadMap["entry"] as? List<*> ?: return null
        val firstEntry = entryList.firstOrNull() as? Map<*, *> ?: return null
        val changesList = firstEntry["changes"] as? List<*> ?: return null
        val firstChange = changesList.firstOrNull() as? Map<*, *> ?: return null
        val valueMap = firstChange["value"] as? Map<*, *> ?: return null
        val messagesList = valueMap["messages"] as? List<*> ?: return null
        val firstMessage = messagesList.firstOrNull() as? Map<*, *> ?: return null
        val fromNumber = firstMessage["from"] as? String ?: return null
        val textMap = firstMessage["text"] as? Map<*, *> ?: return null
        val messageBody = textMap["body"] as? String ?: return null

        return WhatsAppWebhookMessage(fromNumber, messageBody)
    }
}
