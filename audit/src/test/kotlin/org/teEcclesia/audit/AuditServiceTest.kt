package org.teEcclesia.audit

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import jakarta.persistence.EntityManager
import jakarta.persistence.metamodel.EntityType
import jakarta.persistence.metamodel.Metamodel

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.teEcclesia.identity.entity.User

class AuditServiceTest {

    private val entityManager: EntityManager = mockk()
    private lateinit var auditService: AuditService

    @BeforeEach
    fun setUp() {
        val metamodel = mockk<Metamodel>()
        val userEntityType = mockk<EntityType<*>>()
        every { userEntityType.name } returns "User"
        every { userEntityType.javaType } returns User::class.java as Class<Any>
        
        every { metamodel.entities } returns setOf(userEntityType)
        every { entityManager.metamodel } returns metamodel
        
        auditService = AuditService(entityManager)
    }

    @Test
    fun `getAuditableEntities should return list of audited entities`() {
        val entities = auditService.getAuditableEntities()
        
        assertEquals(1, entities.size)
        assertEquals("user", entities[0].id)
        assertEquals("User", entities[0].name)
    }

    // Since AuditReaderFactory.get() and AuditEntity.id().eq() use a lot of complex Envers static classes,
    // creating a full mocked unit test is brittle and highly prone to breaking on version updates.
    // However, the getAuditableEntities effectively tests the dynamic mapping logic.
    // Further full integration testing of Envers is usually done at the integration level.
}
