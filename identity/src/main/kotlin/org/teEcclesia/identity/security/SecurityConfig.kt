package org.teEcclesia.identity.security

import org.teEcclesia.identity.security.handler.CustomAuthenticationEntryPoint
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtFilter: JwtFilter,
    private val authenticationEntryPoint: CustomAuthenticationEntryPoint,
    ) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.csrf { it.disable() }
            .headers { headers ->
                headers.frameOptions { it.disable() }
            }
            .authorizeHttpRequests {
                it.requestMatchers("/api/v1/identity/auth/logout").authenticated()
                it.requestMatchers(
                    "/api/v1/identity/auth/**",
                    "/v3/api-docs",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/error",
                    "/"
                )
                    .permitAll()
                it.anyRequest().authenticated()
            }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter::class.java)
            .exceptionHandling { exception ->
                exception.authenticationEntryPoint(authenticationEntryPoint)
            }
        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun authenticationManager(authConfig: AuthenticationConfiguration): AuthenticationManager {
        return authConfig.authenticationManager
    }

}