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
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/**
 * 外部クライアントの DI 設定（Google Speech / Gemini）。
 */
@Configuration
@Profile("!test")
class ExternalClientConfig {

    @Bean
    fun speechClient(
        speechProperties: GoogleSpeechProperties,
    ): SpeechClient {
        val settingsBuilder = SpeechSettings.newBuilder()

        FileInputStream(speechProperties.credentialsPath).use { input: FileInputStream ->
            val credentials = GoogleCredentials.fromStream(input)
            val provider = FixedCredentialsProvider.create(credentials)
            settingsBuilder.setCredentialsProvider(provider)
        }

        return SpeechClient.create(settingsBuilder.build())
    }

    @Bean
    fun speechTranscriber(
        speechClient: SpeechClient,
        speechProperties: GoogleSpeechProperties,
    ): SpeechTranscriber =
        GoogleSpeechTranscriber(
            speechClient = speechClient,
            defaultLanguageCode = speechProperties.defaultLanguage,
            timeout = speechProperties.timeoutSeconds.seconds,
        )

    @Bean
    fun aiMemoFormatter(
        geminiProperties: GeminiProperties,
    ): AiMemoFormatter =
        GeminiAiMemoFormatter(
            apiKey = geminiProperties.apiKey,
            model = geminiProperties.model,
            timeout = Duration.ofSeconds(geminiProperties.timeoutSeconds),
            baseUrl = geminiProperties.baseUrl,
        )
}
