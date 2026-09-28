package org.teEcclesia.identity.security.handler

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.teEcclesia.i18n.I18nService

@Component
class AuthErrorResponder(
    private val i18nService: I18nService,
    private val objectMapper: ObjectMapper = ObjectMapper()
) {

    fun handleJwtExpired(response: HttpServletResponse) {
        val message = i18nService.getMessage(
            key = "error.auth.token_expired",
            defaultMessage = "انتهت صلاحية الجلسة، يرجى تسجيل الدخول مرة أخرى"
        )
        val apiError = ApiErrorResponse(
            status = 1001,
            message = message
        )
        sendErrorResponse(response, apiError)
    }

    fun handleInvalidToken(response: HttpServletResponse) {
        val message = i18nService.getMessage(
            key = "error.auth.token_invalid",
            defaultMessage = "رمز التحقق غير صالح"
        )
        val apiError = ApiErrorResponse(
            status = 1002,
            message = message
        )
        sendErrorResponse(response, apiError)
    }

    fun handleGeneralAuthError(response: HttpServletResponse, customKeyOrMessage: String? = null) {
        val key = when (customKeyOrMessage) {
            "error.auth.registration_token_not_allowed" -> "error.auth.registration_token_not_allowed"
            else -> "error.auth.unauthorized"
        }
        val fallback = when (key) {
            "error.auth.registration_token_not_allowed" -> "لا يمكن استخدام رمز التسجيل مع نقطة النهاية هذه"
            else -> "غير مصرح لك بالوصول"
        }
        val message = i18nService.getMessage(key = key, defaultMessage = fallback)
        val apiError = ApiErrorResponse(
            status = HttpServletResponse.SC_UNAUTHORIZED,
            message = message
        )
        sendErrorResponse(response, apiError)
    }

    private fun sendErrorResponse(
        response: HttpServletResponse,
        apiError: ApiErrorResponse,
    ) {
        response.contentType = "application/json;charset=UTF-8"
        response.characterEncoding = "UTF-8"
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.writer.write(objectMapper.writeValueAsString(apiError))
    }
}