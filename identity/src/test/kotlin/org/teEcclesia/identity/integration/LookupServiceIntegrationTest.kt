package org.teEcclesia.identity.integration

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import org.teEcclesia.identity.IdentityTestApplication
import org.teEcclesia.identity.entity.enums.UserRole
import org.teEcclesia.identity.entity.lookups.EducationalStage
import org.teEcclesia.identity.repository.EducationalStageRepository
import org.teEcclesia.identity.service.LookupService

@SpringBootTest(classes = [IdentityTestApplication::class])
@ActiveProfiles("test")
class LookupServiceIntegrationTest {

    @Autowired
    private lateinit var lookupService: LookupService

    @Autowired
    private lateinit var educationalStageRepository: EducationalStageRepository

    @BeforeEach
    fun setUp() {
        educationalStageRepository.deleteAll()
    }

    @Test
    fun `getEducationalStages should filter out khadem only stages when forRole is MAKHDOOM`() {
        val generalStage = educationalStageRepository.save(
            EducationalStage(nameAr = "مرحلة مخدومين", nameEn = "Makhdoom Stage", isKhademOnly = false)
        )
        val khademStage = educationalStageRepository.save(
            EducationalStage(nameAr = "إعداد خدام", nameEn = "Khadem Preparation", isKhademOnly = true)
        )

        val pageable = PageRequest.of(0, 10)
        val responseForMakhdoom = lookupService.getEducationalStages("ar", null, UserRole.MAKHDOOM, pageable)

        assertThat(responseForMakhdoom.content.map { it.id }).containsExactly(generalStage.id)
        assertThat(responseForMakhdoom.content.map { it.id }).doesNotContain(khademStage.id)
    }

    @Test
    fun `getEducationalStages should return all stages including khadem only when forRole is KHADEM`() {
        val generalStage = educationalStageRepository.save(
            EducationalStage(nameAr = "مرحلة مخدومين", nameEn = "Makhdoom Stage", isKhademOnly = false)
        )
        val khademStage = educationalStageRepository.save(
            EducationalStage(nameAr = "إعداد خدام", nameEn = "Khadem Preparation", isKhademOnly = true)
        )

        val pageable = PageRequest.of(0, 10)
        val responseForKhadem = lookupService.getEducationalStages("ar", null, UserRole.KHADEM, pageable)

        assertThat(responseForKhadem.content.map { it.id }).containsExactly(generalStage.id, khademStage.id)
    }
}
