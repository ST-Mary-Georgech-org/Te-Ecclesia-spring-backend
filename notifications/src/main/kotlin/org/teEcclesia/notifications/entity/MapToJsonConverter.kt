package org.teEcclesia.notifications.entity

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class MapToJsonConverter : AttributeConverter<Map<String, String>, String> {

    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    override fun convertToDatabaseColumn(attribute: Map<String, String>?): String {
        return if (attribute.isNullOrEmpty()) {
            "{}"
        } else {
            runCatching { objectMapper.writeValueAsString(attribute) }.getOrDefault("{}")
        }
    }

    override fun convertToEntityAttribute(dbData: String?): Map<String, String> {
        return if (dbData.isNullOrBlank()) {
            emptyMap()
        } else {
            runCatching {
                objectMapper.readValue(dbData, object : TypeReference<Map<String, String>>() {})
            }.getOrDefault(emptyMap())
        }
    }
}
