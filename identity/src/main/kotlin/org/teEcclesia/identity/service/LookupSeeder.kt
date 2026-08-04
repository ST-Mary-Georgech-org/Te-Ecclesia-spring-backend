package org.teEcclesia.identity.service

import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import org.teEcclesia.identity.entity.SystemSetting
import org.teEcclesia.identity.entity.enums.SettingKey
import org.teEcclesia.identity.entity.lookups.*
import org.teEcclesia.identity.repository.*

@Component
class LookupSeeder(
    private val rankRepository: RankRepository,
    private val educationalStageRepository: EducationalStageRepository,
    private val systemSettingRepository: SystemSettingRepository
) : CommandLineRunner {

    override fun run(vararg args: String) {
        seedRanks()
        seedEducationalData()
        seedSystemSettings()
    }

    private fun seedSystemSettings() {
        if (!systemSettingRepository.existsById(SettingKey.PARENTS_WHATSAPP_LINK)) {
            systemSettingRepository.save(
                SystemSetting(
                    key = SettingKey.PARENTS_WHATSAPP_LINK,
                    value = null,
                    description = "رابط مجموعة واتساب العامة لأولياء الأمور"
                )
            )
        }
    }

    private fun seedRanks() {
        if (rankRepository.count() == 0L) {
            val ranks = listOf(
                Rank(nameAr = "إبصالطس", nameEn = "Epsaltos", codeLetter = 'B'),
                Rank(nameAr = "أغنسطوس", nameEn = "Ognostos", codeLetter = 'C'),
                Rank(nameAr = "إيبوذياكون", nameEn = "Epodkiakon", codeLetter = 'D'),
                Rank(nameAr = "دياكون", nameEn = "Diakon", codeLetter = 'E'),
                Rank(nameAr = "أرشي دياكون", nameEn = "Archdiakon", codeLetter = 'F')
            )
            rankRepository.saveAll(ranks)
        }
    }

    private fun seedEducationalData() {
        if (educationalStageRepository.count() == 0L) {
            val nursery = EducationalStage(nameAr = "حضانة", nameEn = "Nursery")
            val primary = EducationalStage(nameAr = "ابتدائي", nameEn = "Primary")
            val preparatory = EducationalStage(nameAr = "إعدادي", nameEn = "Preparatory")
            val secondary = EducationalStage(nameAr = "ثانوي", nameEn = "Secondary")
            val youth = EducationalStage(nameAr = "شباب", nameEn = "Youth")
            val graduate = EducationalStage(nameAr = "خريجين", nameEn = "Graduate")
            val families = EducationalStage(nameAr = "أسر", nameEn = "Families")
            val deaconsSchool = EducationalStage(nameAr = "مدرسة الشمامسة", nameEn = "Deacons School", isKhademOnly = true)

            nursery.years.addAll(createYearsForStage(nursery, 3))
            primary.years.addAll(createYearsForStage(primary, 6))
            preparatory.years.addAll(createYearsForStage(preparatory, 3))
            secondary.years.addAll(createYearsForStage(secondary, 3))
            youth.years.addAll(createYearsForStage(youth, 7))

            val stages = listOf(nursery, primary, preparatory, secondary, youth, graduate, families, deaconsSchool)
            educationalStageRepository.saveAll(stages)
        } else if (!educationalStageRepository.existsByNameAr("مدرسة الشمامسة")) {
            val deaconsSchool = EducationalStage(nameAr = "مدرسة الشمامسة", nameEn = "Deacons School", isKhademOnly = true)
            educationalStageRepository.save(deaconsSchool)
        }
    }

    private fun createYearsForStage(stage: EducationalStage, count: Int): List<EducationalYear> {
        val ranksAr = listOf("الأولى", "الثانية", "الثالثة", "الرابعة", "الخامسة", "السادسة", "السابعة")
        val ranksEn = listOf("First", "Second", "Third", "Fourth", "Fifth", "Sixth", "Seventh")

        return (0 until count).map { index ->
            EducationalYear(
                nameAr = "السنة ${ranksAr[index]}",
                nameEn = "${ranksEn[index]} Grade",
                stage = stage,
                whatsAppLink = null
            )
        }
    }
}
