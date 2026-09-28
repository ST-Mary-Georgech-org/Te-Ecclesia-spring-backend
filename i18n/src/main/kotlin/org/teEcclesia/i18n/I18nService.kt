package org.teEcclesia.i18n

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Service
import java.util.Locale

@Service
class I18nService(private val messageSource: MessageSource) {

    val logger: Logger = LoggerFactory.getLogger(I18nService::class.java)

    fun getMessage(
        key: String,
        locale: Locale = currentLocale(),
        defaultMessage: String? = null,
        vararg args: Any
    ): String {
        return try {
            messageSource.getMessage(key, args, locale)
        } catch (e: Exception) {
            logger.error("Error retrieving message for key '$key' and locale '$locale': ${e.message}")
            defaultMessage ?: key
        }
    }

    fun currentLocale(): Locale {
        val locale = try {
            LocaleContextHolder.getLocale()
        } catch (_: Exception) {
            Locale.forLanguageTag("ar")
        }
        return if (locale.language.isNotBlank()) locale else Locale.forLanguageTag("ar")
    }
}
