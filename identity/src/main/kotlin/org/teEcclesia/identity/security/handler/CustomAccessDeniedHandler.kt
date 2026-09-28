package org.teEcclesia.identity.security.handler

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import org.teEcclesia.i18n.I18nService

@Component
class CustomAccessDeniedHandler(
    private val i18nService: I18nService,
    private val objectMapper: ObjectMapper = ObjectMapper()
) : AccessDeniedHandler {

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException
    ) {
        val message = i18nService.getMessage(
            key = "error.auth.forbidden",
            defaultMessage = "ليس لديك الصلاحية لتنفيذ هذا الإجراء"
        )
        val apiError = ApiErrorResponse(
            status = HttpServletResponse.SC_FORBIDDEN,
            message = message
        )

        response.contentType = "application/json;charset=UTF-8"
        response.characterEncoding = "UTF-8"
        response.status = HttpServletResponse.SC_FORBIDDEN
        response.writer.write(objectMapper.writeValueAsString(apiError))
    }
}
