package com.assari.voicebooklm.config

import com.assari.voicebooklm.infrastructure.api.ai.GeminiAiMemoFormatter
import com.assari.voicebooklm.infrastructure.api.speech.GoogleSpeechTranscriber
import com.assari.voicebooklm.infrastructure.api.storage.GcsStorageService
import com.assari.voicebooklm.domain.gateway.MemoFormatter
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.speech.v1.SpeechClient
import com.google.cloud.speech.v1.SpeechSettings
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import java.io.FileInputStream
import java.time.Duration
import kotlin.time.Duration.Companion.seconds
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/**
 * 外部クライアントの DI 設定（Google Cloud Storage / Speech-to-Text / Gemini）
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
     * Google Cloud Storage クライアント
     */
    @Bean
    fun storageClient(
        googleCredentials: GoogleCredentials,
        cloudProperties: GoogleCloudProperties,
    ): Storage =
        StorageOptions.newBuilder()
            .setCredentials(googleCredentials)
            .setProjectId(cloudProperties.projectId)
            .build()
            .service

    /**
     * GCS ストレージサービス
     */
    @Bean
    fun gcsStorageService(
        storageClient: Storage,
        cloudProperties: GoogleCloudProperties,
    ): GcsStorageService =
        GcsStorageService(
            storage = storageClient,
            bucketName = cloudProperties.storage.bucketName,
        )

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

    /**
     * Speech-to-Text 文字起こしサービス
     */
    @Bean
    fun speechTranscriber(
        speechClient: SpeechClient,
        gcsStorageService: GcsStorageService,
        cloudProperties: GoogleCloudProperties,
    ): GoogleSpeechTranscriber =
        GoogleSpeechTranscriber(
            speechClient = speechClient,
            gcsStorageService = gcsStorageService,
            defaultLanguageCode = cloudProperties.speech.defaultLanguage,
            timeout = cloudProperties.speech.timeoutSeconds.seconds,
        )

    /**
     * Gemini AI メモ整形サービス
     */
    @Bean
    fun aiMemoFormatter(
        geminiProperties: GeminiProperties,
    ): MemoFormatter =
        GeminiAiMemoFormatter(
            apiKey = geminiProperties.apiKey,
            model = geminiProperties.model,
            timeout = Duration.ofSeconds(geminiProperties.timeoutSeconds),
            baseUrl = geminiProperties.baseUrl,
        )
}
