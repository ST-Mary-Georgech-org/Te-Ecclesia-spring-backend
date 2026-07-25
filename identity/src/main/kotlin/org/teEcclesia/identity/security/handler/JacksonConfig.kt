package org.teEcclesia.identity.security.handler

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JacksonConfig {
    @Bean
    fun objectMapper(): ObjectMapper = ObjectMapper().findAndRegisterModules()
}