package org.teEcclesia.identity.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.teEcclesia.i18n.I18nService
import org.teEcclesia.identity.security.handler.CustomAccessDeniedHandler
import org.teEcclesia.identity.security.handler.CustomAuthenticationEntryPoint
import java.util.Locale

class SecurityHandlersTest {

    private lateinit var i18nService: I18nService
    private val objectMapper = ObjectMapper()
    private lateinit var entryPoint: CustomAuthenticationEntryPoint
    private lateinit var accessDeniedHandler: CustomAccessDeniedHandler

    @BeforeEach
    fun setUp() {
        val messageSource = ReloadableResourceBundleMessageSource().apply {
            setBasename("classpath:messages")
            setDefaultEncoding("UTF-8")
            setFallbackToSystemLocale(false)
            setDefaultLocale(Locale.forLanguageTag("ar"))
        }
        i18nService = I18nService(messageSource)
        entryPoint = CustomAuthenticationEntryPoint(i18nService, objectMapper)
        accessDeniedHandler = CustomAccessDeniedHandler(i18nService, objectMapper)
        LocaleContextHolder.setLocale(Locale.forLanguageTag("ar"))
    }

    @AfterEach
    fun tearDown() {
        LocaleContextHolder.resetLocaleContext()
    }

    @Test
    fun `CustomAuthenticationEntryPoint returns 401 with Arabic message by default`() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()

        entryPoint.commence(request, response, BadCredentialsException("Unauthenticated"))

        assertThat(response.status).isEqualTo(401)
        assertThat(response.contentType).contains("application/json")
        assertThat(response.characterEncoding).isEqualTo("UTF-8")

        val responseBody = response.contentAsString
        assertThat(responseBody).contains("يجب تسجيل الدخول أولاً للمتابعة")
        assertThat(responseBody).contains("\"status\":401")
    }

    @Test
    fun `CustomAccessDeniedHandler returns 403 with Arabic message by default`() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()

        accessDeniedHandler.handle(request, response, AccessDeniedException("Forbidden"))

        assertThat(response.status).isEqualTo(403)
        assertThat(response.contentType).contains("application/json")
        assertThat(response.characterEncoding).isEqualTo("UTF-8")

        val responseBody = response.contentAsString
        assertThat(responseBody).contains("ليس لديك الصلاحية لتنفيذ هذا الإجراء")
        assertThat(responseBody).contains("\"status\":403")
    }
}
