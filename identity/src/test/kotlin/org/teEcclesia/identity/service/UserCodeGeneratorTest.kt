package org.teEcclesia.identity.service

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.teEcclesia.identity.entity.User
import org.teEcclesia.identity.entity.enums.Gender
import org.teEcclesia.identity.entity.enums.UserRole
import org.teEcclesia.identity.repository.UserRepository
import java.time.LocalDate

class UserCodeGeneratorTest {

    private val userRepository: UserRepository = mockk()
    private val generator = UserCodeGenerator(userRepository)

    @Test
    fun `should generate code with correct format for new priest`() {
        // Arrange
        val user = mockk<User>()
        every { user.role } returns UserRole.KAHEN
        every { user.gender } returns Gender.MALE
        every { user.ordinationProfile } returns null

        every { user.nationalId } returns "29901011234567" // Birth year 99, last 4 digits 4567

        val yearStr = LocalDate.now().year.toString().takeLast(2)
        val prefix = "H${yearStr}99"
        
        every { userRepository.existsByCodeLike("_${yearStr}994567") } returns false

        // Act
        val code = generator.generateCode(user)

        // Assert
        assertTrue(code.startsWith(prefix))
        assertEquals("${prefix}4567", code)
    }

    @Test
    fun `should increment sequence when previous codes exist`() {
        // Arrange
        val user = mockk<User>()
        every { user.role } returns UserRole.MAKHDOOM
        every { user.gender } returns Gender.FEMALE
        every { user.ordinationProfile } returns null

        every { user.nationalId } returns "30501011234567" // Birth year 05, last 4 digits 4567

        val yearStr = LocalDate.now().year.toString().takeLast(2)
        val prefix = "G${yearStr}05"
        
        // Mock that 4567 already exists, but 4568 does not
        every { userRepository.existsByCodeLike("_${yearStr}054567") } returns true
        every { userRepository.existsByCodeLike("_${yearStr}054568") } returns false

        // Act
        val code = generator.generateCode(user)

        // Assert
        assertEquals("${prefix}4568", code)
    }
}
