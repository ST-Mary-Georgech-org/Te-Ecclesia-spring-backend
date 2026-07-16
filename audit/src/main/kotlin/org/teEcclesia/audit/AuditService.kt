package org.teEcclesia.audit

import org.hibernate.envers.AuditReaderFactory
import org.hibernate.envers.query.AuditEntity
import org.hibernate.envers.DefaultRevisionEntity
import org.hibernate.envers.RevisionType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.util.UUID

@Service
class AuditService(private val entityManager: EntityManager) {

    private val auditedEntitiesMap: Map<String, Class<*>> by lazy {
        entityManager.metamodel.entities
            .filter { it.javaType.isAnnotationPresent(org.hibernate.envers.Audited::class.java) }
            .associate { it.name.lowercase() to it.javaType }
    }

    fun getAuditableEntities(): List<AuditableEntityDto> {
        return auditedEntitiesMap.map { (key, clazz) ->
            AuditableEntityDto(
                id = key,
                name = clazz.simpleName
            )
        }
    }

    private fun resolveEntityClass(entityTypeId: String): Class<*> {
        return auditedEntitiesMap[entityTypeId.lowercase()]
            ?: throw IllegalArgumentException("Unknown or non-audited entity type: $entityTypeId")
    }

    private fun parseEntityId(clazz: Class<*>, entityIdStr: String): Any {
        val idType = entityManager.metamodel.entity(clazz).idType.javaType
        return when (idType) {
            UUID::class.java -> UUID.fromString(entityIdStr)
            Long::class.java, Long::class.javaObjectType -> entityIdStr.toLong()
            Int::class.java, Int::class.javaObjectType -> entityIdStr.toInt()
            else -> entityIdStr
        }
    }

    fun getAudits(entityType: String, entityIdStr: String?, pageable: Pageable): Page<AuditDto> {
        val clazz = resolveEntityClass(entityType)
        val auditReader = AuditReaderFactory.get(entityManager)
        
        val parsedId = entityIdStr?.let { parseEntityId(clazz, it) }

        val countQuery = auditReader.createQuery().forRevisionsOfEntity(clazz, false, true)
        countQuery.addProjection(AuditEntity.id().count())
        if (parsedId != null) {
            countQuery.add(AuditEntity.id().eq(parsedId))
        }
        val totalElements = countQuery.singleResult as Long

        val query = auditReader.createQuery().forRevisionsOfEntity(clazz, false, true)
        if (parsedId != null) {
            query.add(AuditEntity.id().eq(parsedId))
        }
        
        query.addOrder(AuditEntity.revisionNumber().desc())
        query.setFirstResult(pageable.offset.toInt())
        query.setMaxResults(pageable.pageSize)
        
        val results = query.resultList as List<Array<Any>>
        val dtoList = results.map { row ->
            val entity = row[0]
            val revEntity = row[1] as DefaultRevisionEntity
            val revType = row[2] as RevisionType
            AuditDto(
                revisionId = revEntity.id,
                timestamp = revEntity.timestamp,
                operation = revType.name,
                entityData = entity
            )
        }
        
        return PageImpl(dtoList, pageable, totalElements)
    }

    @Transactional
    fun revertToRevision(entityType: String, entityIdStr: String, revisionId: Int) {
        val clazz = resolveEntityClass(entityType)
        val parsedId = parseEntityId(clazz, entityIdStr)
        val auditReader = AuditReaderFactory.get(entityManager)
        val historicalEntity = auditReader.find(clazz, parsedId, revisionId) 
            ?: throw EntityNotFoundException("Revision $revisionId not found for $entityType with ID $entityIdStr")
        entityManager.merge(historicalEntity)
    }
}
