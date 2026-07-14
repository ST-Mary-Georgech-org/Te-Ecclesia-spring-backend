package org.teEcclesia.notifications.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration

@Configuration
class FirebaseConfig {
    private val logger = LoggerFactory.getLogger(FirebaseConfig::class.java)

    @PostConstruct
    fun initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                val credentialsStream = this::class.java.classLoader.getResourceAsStream("teEcclesia-firebase-adminsdk.json")
                    ?: error("Firebase credentials not found")

                val options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentialsStream))
                    .build()

                FirebaseApp.initializeApp(options)
                logger.info("FirebaseApp initialized successfully.")
            }
        } catch (e: Exception) {
            logger.error("Failed to initialize FirebaseApp", e)
        }
    }
}
