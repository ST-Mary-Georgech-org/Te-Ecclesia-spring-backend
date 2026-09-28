package org.teEcclesia.identity.config

import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import org.springframework.web.servlet.LocaleResolver
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver
import java.util.Locale

@Configuration
class WebLocaleConfig {

    @Bean
    fun localeResolver(): LocaleResolver {
        val resolver = AcceptHeaderLocaleResolver()
        resolver.setDefaultLocale(Locale.forLanguageTag("ar"))
        resolver.setSupportedLocales(listOf(Locale.forLanguageTag("ar"), Locale.forLanguageTag("en")))
        return resolver
    }

    @Bean
    fun validator(messageSource: MessageSource): LocalValidatorFactoryBean {
        val bean = LocalValidatorFactoryBean()
        bean.setValidationMessageSource(messageSource)
        return bean
    }
}
