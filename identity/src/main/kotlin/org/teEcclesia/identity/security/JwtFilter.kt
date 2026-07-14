package org.teEcclesia.identity.security

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.MalformedJwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.teEcclesia.identity.security.handler.AuthErrorResponder
import org.teEcclesia.identity.service.UserService
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtFilter(
    private val jwtUtil: JwtUtil,
    private val userService: UserService,
    private val authErrorResponder: AuthErrorResponder
) : OncePerRequestFilter() {

    val pathsToSkip = listOf(
        "/api/v1/identity/auth/**",
        "/v3/api-docs",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/error",
        "/"
    )

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.servletPath ?: return false
        val matcher = AntPathMatcher()
        if (matcher.match("/api/v1/identity/auth/logout", path)) return false
        return pathsToSkip.any { matcher.match(it, path) }
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val authHeader = request.getHeader("Authorization")

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response)
                return
            }

            val token = authHeader.substring(7)
            val userId = jwtUtil.extractUserId(token)
            if (userId != null && SecurityContextHolder.getContext().authentication == null) {
                if (jwtUtil.validateAccessToken(token) &&
                    jwtUtil.validateTokenForUser(token, userId)
                ) {
                    if (!userService.existById(userId)) throw IllegalStateException("Not authorized")
                    val authToken = UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        emptyList()
                    )
                    authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                    SecurityContextHolder.getContext().authentication = authToken
                }
            }

            filterChain.doFilter(request, response)
        } catch (_: ExpiredJwtException) {
            authErrorResponder.handleJwtExpired(response)
        } catch (_: MalformedJwtException) {
            authErrorResponder.handleInvalidToken(response)
        } catch (e: Exception) {
            logger.info("Error processing JWT: ${e.message}")
            authErrorResponder.handleGeneralAuthError(response)
        }
    }
}