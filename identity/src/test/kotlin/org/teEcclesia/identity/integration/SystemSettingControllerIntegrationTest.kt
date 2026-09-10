package org.teEcclesia.identity.integration

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.teEcclesia.identity.IdentityTestApplication
import org.teEcclesia.identity.api.controller.SystemSettingController
import org.teEcclesia.identity.api.dto.request.UpdateAcademicYearRequest
import org.teEcclesia.identity.entity.User
import org.teEcclesia.identity.entity.enums.Gender
import org.teEcclesia.identity.entity.enums.UserRole
import org.teEcclesia.identity.entity.enums.UserStatus
import org.teEcclesia.identity.exception.UnauthorizedException
import org.teEcclesia.identity.repository.UserRepository
import org.teEcclesia.identity.service.SystemSettingService
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@SpringBootTest(classes = [IdentityTestApplication::class])
@ActiveProfiles("test")
class SystemSettingControllerIntegrationTest {

    @Autowired
    private lateinit var systemSettingController: SystemSettingController

    @Autowired
    private lateinit var systemSettingService: SystemSettingService

    @Autowired
    private lateinit var userRepository: UserRepository

    private lateinit var adminUser: User
    private lateinit var regularUser: User

    @BeforeEach
    fun setUp() {
        adminUser = userRepository.save(
            User(
                id = UUID.randomUUID(),
                firstName = "Admin",
                secondName = "Sec",
                thirdName = "Third",
                lastName = "Last",
                displayName = "Admin User",
                nationalId = "29901011234568",
                phone = "01000000001",
                email = "admin-sys@test.com",
                passwordHash = "hash",
                buildingNo = "1",
                street = "Street",
                area = "Area",
                floor = "1",
                specialMark = "Mark",
                gender = Gender.MALE,
                status = UserStatus.APPROVED,
                role = UserRole.ADMIN,
                createdAt = Instant.now(),
                birthDate = LocalDate.of(1990, 1, 1)
            )
        )

        regularUser = userRepository.save(
            User(
                id = UUID.randomUUID(),
                firstName = "Makhdoom",
                secondName = "Sec",
                thirdName = "Third",
                lastName = "Last",
                displayName = "Regular User",
                nationalId = "29901011234569",
                phone = "01000000002",
                email = "user-sys@test.com",
                passwordHash = "hash",
                buildingNo = "1",
                street = "Street",
                area = "Area",
                floor = "1",
                specialMark = "Mark",
                gender = Gender.MALE,
                status = UserStatus.APPROVED,
                role = UserRole.MAKHDOOM,
                createdAt = Instant.now(),
                birthDate = LocalDate.of(2005, 1, 1)
            )
        )
    }

    @Test
    fun `getCurrentAcademicYear returns current year`() {
        systemSettingService.updateCurrentAcademicYear(2026)
        val response = systemSettingController.getCurrentAcademicYear()
        assertThat(response.statusCode.is2xxSuccessful).isTrue()
        assertThat(response.body?.academicYear).isEqualTo(2026)
    }

    @Test
    fun `updateCurrentAcademicYear allows admin to change academic year`() {
        val response = systemSettingController.updateCurrentAcademicYear(
            callerId = adminUser.id,
            request = UpdateAcademicYearRequest(year = 2028)
        )
        assertThat(response.statusCode.is2xxSuccessful).isTrue()
        assertThat(response.body?.academicYear).isEqualTo(2028)
        assertThat(systemSettingService.getCurrentAcademicYear()).isEqualTo(2028)
    }

    @Test
    fun `updateCurrentAcademicYear throws UnauthorizedException for non-admin`() {
        assertThrows<UnauthorizedException> {
            systemSettingController.updateCurrentAcademicYear(
                callerId = regularUser.id,
                request = UpdateAcademicYearRequest(year = 2029)
            )
        }
    }
}
