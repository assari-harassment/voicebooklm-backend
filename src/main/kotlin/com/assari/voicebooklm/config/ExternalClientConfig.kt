package com.assari.voicebooklm.config

import com.assari.voicebooklm.infrastructure.api.ai.GeminiAiMemoFormatter
import com.assari.voicebooklm.infrastructure.api.speech.GoogleSpeechTranscriber
import com.assari.voicebooklm.usecase.memo.client.AiMemoFormatter
import com.assari.voicebooklm.usecase.memo.client.SpeechTranscriber
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.speech.v1.SpeechClient
import com.google.cloud.speech.v1.SpeechSettings
import java.io.FileInputStream
import java.time.Duration
import kotlin.time.Duration.Companion.seconds
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

/**
 * 外部クライアントの DI 設定（Google Speech / Gemini）。
 */
@Configuration
class ExternalClientConfig {

    @Bean
    @ConditionalOnProperty(name = ["google.cloud.speech.enabled"], havingValue = "true")
    fun speechClient(
        @Value("\${google.cloud.speech.credentials-path:}") credentialsPath: String,
    ): SpeechClient {
        val settingsBuilder = SpeechSettings.newBuilder()

        if (credentialsPath.isNotBlank()) {
            FileInputStream(credentialsPath).use { input: FileInputStream ->
                val credentials = GoogleCredentials.fromStream(input)
                val provider = FixedCredentialsProvider.create(credentials)
                settingsBuilder.setCredentialsProvider(provider)
            }
        }

        return SpeechClient.create(settingsBuilder.build())
    }

    @Bean
    @ConditionalOnProperty(name = ["google.cloud.speech.enabled"], havingValue = "true")
    fun speechTranscriber(
        speechClient: SpeechClient,
        @Value("\${google.cloud.speech.default-language:ja-JP}") defaultLanguage: String,
        @Value("\${google.cloud.speech.timeout-seconds:60}") timeoutSeconds: Long,
    ): SpeechTranscriber =
        GoogleSpeechTranscriber(
            speechClient = speechClient,
            defaultLanguageCode = defaultLanguage,
            timeout = timeoutSeconds.seconds,
        )

    @Bean
    @ConditionalOnProperty(name = ["gemini.api-key"])
    fun aiMemoFormatter(
        webClient: WebClient,
        @Value("\${gemini.api-key}") apiKey: String,
        @Value("\${gemini.model:gemini-2.0-flash}") model: String,
        @Value("\${gemini.timeout-seconds:60}") timeoutSeconds: Long,
        @Value("\${gemini.base-url:https://generativelanguage.googleapis.com}") baseUrl: String,
    ): AiMemoFormatter =
        GeminiAiMemoFormatter(
            webClient = webClient,
            apiKey = apiKey,
            model = model,
            timeout = Duration.ofSeconds(timeoutSeconds),
            baseUrl = baseUrl,
        )
}
