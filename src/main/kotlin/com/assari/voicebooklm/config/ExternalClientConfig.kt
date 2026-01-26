package com.assari.voicebooklm.config

import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.speech.v1.SpeechClient
import com.google.cloud.speech.v1.SpeechSettings
import java.io.FileInputStream
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/**
 * 外部クライアントの DI 設定（Google Cloud SDK クライアント）
 *
 * GoogleSpeechTranscriber, GeminiAiMemoFormatter は
 * 各クラスに @Component が付与されているため、ここでは SDK クライアントのみ定義。
 */
@Configuration
@Profile("!test")
class ExternalClientConfig {

    /**
     * Google Cloud 認証情報を作成
     */
    @Bean
    fun googleCredentials(
        cloudProperties: GoogleCloudProperties,
    ): GoogleCredentials =
        FileInputStream(cloudProperties.credentialsPath).use { input ->
            GoogleCredentials.fromStream(input)
        }

    /**
     * Speech-to-Text クライアント
     */
    @Bean
    fun speechClient(
        googleCredentials: GoogleCredentials,
    ): SpeechClient {
        val settings = SpeechSettings.newBuilder()
            .setCredentialsProvider(FixedCredentialsProvider.create(googleCredentials))
            .build()
        return SpeechClient.create(settings)
    }
}
