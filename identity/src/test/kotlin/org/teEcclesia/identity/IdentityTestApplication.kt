package org.teEcclesia.identity

import io.mockk.mockk
import org.teEcclesia.events.publisher.TeEcclesiaEventPublisher
import org.teEcclesia.identity.repository.EmailVerificationRepository
import org.teEcclesia.identity.repository.RefreshTokenRepository
import org.teEcclesia.identity.repository.UserRepository
import org.teEcclesia.identity.security.JwtUtil
import org.teEcclesia.identity.service.AuthService
import org.teEcclesia.identity.service.EmailService
import org.teEcclesia.identity.service.UserService
import com.fasterxml.jackson.databind.ObjectMapper
import org.teEcclesia.identity.service.mapper.WhatsAppWebhookMapper
import org.teEcclesia.storage.service.ImageStorageService
import org.teEcclesia.client.ApiClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.teEcclesia.identity.repository.EducationalStageRepository
import org.teEcclesia.identity.repository.EducationalYearRepository
import org.teEcclesia.identity.repository.RankRepository
import org.teEcclesia.identity.service.ParentProfileService
import org.teEcclesia.identity.service.UserCodeGenerator

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackages = ["org.teEcclesia.identity.entity"])
@EnableJpaRepositories(basePackages = ["org.teEcclesia.identity.repository"])
class IdentityTestApplication {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun userCodeGenerator(): UserCodeGenerator = mockk(relaxed = true)

    @Bean
    fun jwtUtil(): JwtUtil = mockk(relaxed = true)

    @Bean
    fun emailService(): EmailService = mockk(relaxed = true)

    @Bean
    fun teEcclesiaEventPublisher(): TeEcclesiaEventPublisher = mockk(relaxed = true)

    @Bean
    fun imageStorageService(): ImageStorageService = mockk(relaxed = true)

    @Bean
    fun apiClient(): ApiClient = mockk(relaxed = true)

    @Bean
    fun objectMapper(): ObjectMapper {
        return ObjectMapper()
    }

    @Bean
    fun whatsAppWebhookMapper(objectMapper: ObjectMapper): WhatsAppWebhookMapper {
        return WhatsAppWebhookMapper(objectMapper)
    }

    @Bean
    fun authService(
        userRepository: UserRepository,
        refreshTokenRepository: RefreshTokenRepository,
        emailVerificationRepository: EmailVerificationRepository,
        emailService: EmailService,
        passwordEncoder: PasswordEncoder,
        jwtUtil: JwtUtil,
        teEcclesiaEventPublisher: TeEcclesiaEventPublisher,
        whatsAppWebhookMapper: WhatsAppWebhookMapper,
        apiClient: ApiClient,
        @Value("\${whatsapp.business-number}") whatsappBusinessNumber: String,
        @Value("\${whatsapp.business-phone}") whatsappBusinessPhone: String,
        @Value("\${whatsapp.app-secret:}") whatsappAppSecret: String,
        @Value("\${whatsapp.webhook.verify-token}") expectedVerifyToken: String,
        @Value("\${WHATSAPP_ACCESS_TOKEN:}") whatsappAccessToken: String,
        rankRepository: RankRepository,
        educationalStageRepository: EducationalStageRepository,
        educationalYearRepository: EducationalYearRepository
    ): AuthService {
        return AuthService(
            userRepository = userRepository,
            refreshTokenRepository = refreshTokenRepository,
            otpRepository = emailVerificationRepository,
            emailService = emailService,
            passwordEncoder = passwordEncoder,
            jwtUtil = jwtUtil,
            teEcclesiaEventPublisher = teEcclesiaEventPublisher,
            whatsAppWebhookMapper = whatsAppWebhookMapper,
            apiClient = apiClient,
            whatsappBusinessNumber = whatsappBusinessNumber,
            whatsappBusinessPhone = whatsappBusinessPhone,
            whatsappAppSecret = whatsappAppSecret,
            expectedVerifyToken = expectedVerifyToken,
            whatsappAccessToken = whatsappAccessToken,
            rankRepository = rankRepository,
            educationalStageRepository = educationalStageRepository,
            educationalYearRepository = educationalYearRepository,
            parentProfileService = parentProfileService()
        )
    }

    @Bean
    fun parentProfileService(): ParentProfileService {
        return mockk<ParentProfileService>(relaxed = true)
    }

    @Bean
    fun userService(
        userRepository: UserRepository,
        imageStorageService: ImageStorageService,
        teEcclesiaEventPublisher: TeEcclesiaEventPublisher,
        @Value("\${identity.resources.profile-image-directory}") profileImageDirectory: String,
        userCodeGenerator: UserCodeGenerator,
        passwordEncoder: PasswordEncoder,
        educationalStageRepository: EducationalStageRepository,
        educationalYearRepository: EducationalYearRepository,
        @Value("\${cdn.endpoint:}") cdnEndpoint: String
    ): UserService {
        return UserService(
            userRepository = userRepository,
            imageStorageService = imageStorageService,
            eventPublisher = teEcclesiaEventPublisher,
            profileImageDirectory = profileImageDirectory,
            userCodeGenerator = userCodeGenerator,
            passwordEncoder = passwordEncoder,
            educationalStageRepository = educationalStageRepository,
            educationalYearRepository = educationalYearRepository,
            areaRepository = mockk(relaxed = true),
            cdnEndpoint = cdnEndpoint,
            parentProfileService = parentProfileService()
        )
    }

}

