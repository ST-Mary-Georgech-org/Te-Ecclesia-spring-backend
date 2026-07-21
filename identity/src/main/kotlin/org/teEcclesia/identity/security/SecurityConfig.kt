package org.teEcclesia.identity.security

import org.teEcclesia.identity.security.handler.CustomAuthenticationEntryPoint
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy

import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtFilter: JwtFilter,
    private val internalHmacFilter: InternalHmacFilter,
    private val authenticationEntryPoint: CustomAuthenticationEntryPoint,
    ) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.csrf { it.disable() }
            .headers { headers ->
                headers.frameOptions { it.disable() }
            }
            .authorizeHttpRequests {
                it.requestMatchers(
                    "/api/v1/identity/auth/logout",
                    "/api/v1/identity/auth/complete-profile",
                    "/api/v1/identity/auth/verify-phone",
                    "/api/v1/identity/auth/verify-email"
                ).authenticated()
                it.requestMatchers(
                    "/api/v1/identity/auth/signup",
                    "/api/v1/identity/auth/priests",
                    "/api/v1/identity/auth/search-parents",
                    "/api/v1/identity/auth/search-makhdooms",
                    "/api/v1/lookups/*",
                    "/api/v1/identity/auth/login",
                    "/api/v1/identity/auth/refresh",
                    "/api/v1/identity/auth/forgot-password",
                    "/api/v1/identity/auth/reset-password",
                    "/api/v1/identity/auth/verify-otp",
                    "/api/v1/identity/auth/resend-otp",
                    "/api/v1/internal/**",
                    "/v3/api-docs",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/error",
                    "/"
                ).permitAll()
                it.anyRequest().authenticated()
            }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .addFilterBefore(internalHmacFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter::class.java)
            .exceptionHandling { exception ->
                exception.authenticationEntryPoint(authenticationEntryPoint)
            }
        return http.build()
    }


    @Bean
    fun authenticationManager(authConfig: AuthenticationConfiguration): AuthenticationManager {
        return authConfig.authenticationManager
    }

}