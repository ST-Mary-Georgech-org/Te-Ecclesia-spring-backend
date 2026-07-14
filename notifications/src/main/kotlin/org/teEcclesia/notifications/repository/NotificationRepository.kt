package org.teEcclesia.notifications.repository

import org.teEcclesia.notifications.entity.Notification
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface NotificationRepository : JpaRepository<Notification, UUID> {

    fun findAllByUserIdOrderBySentAtDesc(userId: UUID, pageable: Pageable): Page<Notification>

    fun countByUserIdAndIsReadFalse(userId: UUID): Long

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    fun markAllAsReadByUserId(userId: UUID): Int
}