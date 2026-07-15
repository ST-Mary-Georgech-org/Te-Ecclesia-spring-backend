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

        val yearStr = LocalDate.now().year.toString().takeLast(2)
        val prefix = "${yearStr}H"
        
        every { userRepository.findMaxCodeByPrefix(prefix) } returns null
        every { userRepository.findByCode("${prefix}000001") } returns null

        // Act
        val code = generator.generateCode(user)

        // Assert
        assertTrue(code.startsWith(prefix))
        assertEquals("${prefix}000001", code)
    }

    @Test
    fun `should increment sequence when previous codes exist`() {
        // Arrange
        val user = mockk<User>()
        every { user.role } returns UserRole.MAKHDOOM
        every { user.gender } returns Gender.FEMALE
        every { user.ordinationProfile } returns null

        val yearStr = LocalDate.now().year.toString().takeLast(2)
        val prefix = "${yearStr}G"
        
        every { userRepository.findMaxCodeByPrefix(prefix) } returns "${prefix}000015"
        every { userRepository.findByCode("${prefix}000016") } returns null

        // Act
        val code = generator.generateCode(user)

        // Assert
        assertEquals("${prefix}000016", code)
    }
}
