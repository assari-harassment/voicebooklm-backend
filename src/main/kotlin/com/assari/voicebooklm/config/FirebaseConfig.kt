package com.assari.voicebooklm.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.io.FileInputStream

/**
 * Firebase Admin SDK の初期化設定
 */
@Configuration
@EnableConfigurationProperties(FirebaseProperties::class)
class FirebaseConfig(
    private val firebaseProperties: FirebaseProperties,
) {
    private val logger = LoggerFactory.getLogger(FirebaseConfig::class.java)

    @PostConstruct
    fun initialize() {
        if (FirebaseApp.getApps().isNotEmpty()) {
            logger.info("Firebase already initialized")
            return
        }

        val options = if (firebaseProperties.credentialsPath.isNotBlank()) {
            // 明示的なサービスアカウントファイルを使用（ローカル開発用）
            logger.info("Initializing Firebase with credentials file: ${firebaseProperties.credentialsPath}")
            FileInputStream(firebaseProperties.credentialsPath).use { serviceAccount ->
                FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setProjectId(firebaseProperties.projectId)
                    .build()
            }
        } else {
            // Application Default Credentials を使用（Cloud Run、GKE など）
            logger.info("Initializing Firebase with Application Default Credentials")
            FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.getApplicationDefault())
                .setProjectId(firebaseProperties.projectId)
                .build()
        }

        FirebaseApp.initializeApp(options)
        logger.info("Firebase initialized successfully for project: ${firebaseProperties.projectId}")
    }
}
