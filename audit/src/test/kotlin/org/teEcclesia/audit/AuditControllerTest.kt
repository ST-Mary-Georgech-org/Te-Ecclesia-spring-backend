package org.teEcclesia.audit

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import org.teEcclesia.identity.entity.User
import org.teEcclesia.identity.entity.enums.UserRole
import org.teEcclesia.identity.service.UserService
import java.util.UUID

class AuditControllerTest {

    private val auditService: AuditService = mockk()
    private val userService: UserService = mockk()
    private val auditController = AuditController(auditService, userService)
    
    @Test
    fun `getAuditableEntities throws Forbidden if user is not admin`() {
        val userId = UUID.randomUUID()
        val user = mockk<User>()
        every { user.role } returns UserRole.GUEST
        every { userService.findById(userId) } returns user
        
        val exception = assertThrows<ResponseStatusException> {
            auditController.getAuditableEntities(userId)
        }
        assertEquals(HttpStatus.FORBIDDEN, exception.statusCode)
    }
    
    @Test
    fun `getAuditableEntities returns entities if user is admin`() {
        val userId = UUID.randomUUID()
        val user = mockk<User>()
        every { user.role } returns UserRole.ADMIN
        every { userService.findById(userId) } returns user
        
        val entities = listOf(AuditableEntityDto("user", "User"))
        every { auditService.getAuditableEntities() } returns entities
        
        val response = auditController.getAuditableEntities(userId)
        
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(entities, response.body)
    }
    
    @Test
    fun `getAudits returns page of audits`() {
        val userId = UUID.randomUUID()
        val user = mockk<User>()
        every { user.role } returns UserRole.ADMIN
        every { userService.findById(userId) } returns user
        
        val pageable = PageRequest.of(0, 10)
        val auditsPage = PageImpl(listOf<AuditDto>())
        every { auditService.getAudits("user", "123", pageable) } returns auditsPage
        
        val response = auditController.getAudits(userId, "user", "123", pageable)
        
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(auditsPage, response.body)
    }
    
    @Test
    fun `revertEntity successfully reverts`() {
        val userId = UUID.randomUUID()
        val user = mockk<User>()
        every { user.role } returns UserRole.ADMIN
        every { userService.findById(userId) } returns user
        
        every { auditService.revertToRevision("user", "123", 1) } returns Unit
        
        val response = auditController.revertEntity(userId, "user", "123", 1)
        
        assertEquals(HttpStatus.OK, response.statusCode)
        verify { auditService.revertToRevision("user", "123", 1) }
    }
}
