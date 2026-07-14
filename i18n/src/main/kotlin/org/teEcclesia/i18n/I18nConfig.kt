package org.teEcclesia.i18n

import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ReloadableResourceBundleMessageSource

@Configuration
class I18nConfig {
    @Bean
    fun messageSource(): MessageSource {
        val rs = ReloadableResourceBundleMessageSource()
        rs.setBasename("classpath:messages")
        rs.setDefaultEncoding("UTF-8")
        rs.setFallbackToSystemLocale(false)
        return rs
    }
}