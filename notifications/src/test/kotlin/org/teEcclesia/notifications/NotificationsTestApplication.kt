package org.teEcclesia.notifications

import org.teEcclesia.notifications.repository.NotificationRepository
import org.teEcclesia.notifications.service.NotificationService
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackages = ["org.teEcclesia.notifications.entity"])
@EnableJpaRepositories(basePackages = ["org.teEcclesia.notifications.repository"])
class NotificationsTestApplication {

    @Bean
    fun notificationService(
        notificationRepository: NotificationRepository
    ): NotificationService {
        return NotificationService(
            notificationRepository = notificationRepository
        )
    }

}