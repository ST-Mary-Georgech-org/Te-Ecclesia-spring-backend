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
import org.teEcclesia.identity.service.LookupService
import com.fasterxml.jackson.databind.ObjectMapper
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
import org.teEcclesia.identity.repository.AreaRepository
import org.teEcclesia.identity.repository.EducationalStageRepository
import org.teEcclesia.identity.repository.EducationalYearRepository
import org.teEcclesia.identity.repository.RankRepository
import org.teEcclesia.identity.repository.SystemSettingRepository
import org.teEcclesia.identity.service.ParentProfileService
import org.teEcclesia.identity.service.SystemSettingService
import org.teEcclesia.identity.service.UserCodeGenerator
import org.teEcclesia.identity.service.UserValidationHelper


@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackages = ["org.teEcclesia.identity.entity"])
@EnableJpaRepositories(basePackages = ["org.teEcclesia.identity.repository"])
class IdentityTestApplication {

    @Bean
    fun systemSettingService(systemSettingRepository: SystemSettingRepository): SystemSettingService {
        return SystemSettingService(systemSettingRepository)
    }

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
    fun userValidationHelper(userRepository: UserRepository): UserValidationHelper {
        return UserValidationHelper(userRepository)
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
        @Value("\${whatsapp.business-phone}") whatsappBusinessPhone: String,
        rankRepository: RankRepository,
        educationalStageRepository: EducationalStageRepository,
        educationalYearRepository: EducationalYearRepository,
        areaRepository: AreaRepository,
        userValidationHelper: UserValidationHelper
    ): AuthService {
        return AuthService(
            userRepository = userRepository,
            refreshTokenRepository = refreshTokenRepository,
            otpRepository = emailVerificationRepository,
            emailService = emailService,
            passwordEncoder = passwordEncoder,
            jwtUtil = jwtUtil,
            teEcclesiaEventPublisher = teEcclesiaEventPublisher,
            whatsappBusinessPhone = whatsappBusinessPhone,
            rankRepository = rankRepository,
            educationalStageRepository = educationalStageRepository,
            educationalYearRepository = educationalYearRepository,
            areaRepository = areaRepository,
            parentProfileService = parentProfileService(),
            imageStorageService = imageStorageService(),
            userValidationHelper = userValidationHelper,
            profileImageDirectory = "test-profiles",
            documentsDirectory = "test-docs"
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
        rankRepository: RankRepository,
        @Value("\${cdn.endpoint:}") cdnEndpoint: String,
        authService: AuthService,
        userValidationHelper: UserValidationHelper,
        systemSettingService: SystemSettingService
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
            rankRepository = rankRepository,
            cdnEndpoint = cdnEndpoint,
            parentProfileService = parentProfileService(),
            documentsDirectory = "test-docs",
            authService = authService,
            userValidationHelper = userValidationHelper,
            systemSettingService = systemSettingService
        )
    }

    @Bean
    fun lookupService(
        rankRepository: RankRepository,
        educationalStageRepository: EducationalStageRepository,
        educationalYearRepository: EducationalYearRepository,
        areaRepository: AreaRepository,
        userRepository: UserRepository
    ): LookupService {
        return LookupService(
            rankRepository = rankRepository,
            educationalStageRepository = educationalStageRepository,
            educationalYearRepository = educationalYearRepository,
            areaRepository = areaRepository,
            userRepository = userRepository
        )
    }

}

