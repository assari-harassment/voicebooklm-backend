package com.assari.voicebooklm.infrastructure.api.speech

import com.assari.voicebooklm.usecase.memo.port.SpeechTranscriptionCommand
import com.google.cloud.speech.v1.RecognitionAudio
import com.google.cloud.speech.v1.RecognitionConfig
import com.google.cloud.speech.v1.RecognizeResponse
import com.google.cloud.speech.v1.SpeechClient
import com.google.cloud.speech.v1.SpeechRecognitionAlternative
import com.google.cloud.speech.v1.SpeechRecognitionResult
import io.mockk.every
import io.mockk.mockk
import java.time.Duration
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * GoogleSpeechTranscriber のフォールバック/成功を検証。
 */
class GoogleSpeechTranscriberTest {

    @Test
    fun `認識成功でテキストを返す`() {
        val speechClient = mockk<SpeechClient>()
        every { speechClient.recognize(any<RecognitionConfig>(), any<RecognitionAudio>()) } returns successResponse("hello world")

        val transcriber = GoogleSpeechTranscriber(
            speechClient = speechClient,
            defaultLanguageCode = "en-US",
            fallbackText = "[fallback]",
            timeout = kotlin.time.Duration.parse("5s"),
        )

        val transcription = runBlocking {
            transcriber.transcribe(
                SpeechTranscriptionCommand(
                    userId = java.util.UUID.randomUUID(),
                    audio = ByteArray(1) { 1 },
                    mimeType = "audio/wav",
                    languageCode = null,
                ),
            )
        }

        assertEquals("hello world", transcription.text)
        assertEquals("en-US", transcription.languageCode)
    }

    @Test
    fun `エラー時はフォールバックを返す`() {
        val speechClient = mockk<SpeechClient>()
        every { speechClient.recognize(any<RecognitionConfig>(), any<RecognitionAudio>()) } throws RuntimeException("API down")

        val transcriber = GoogleSpeechTranscriber(
            speechClient = speechClient,
            defaultLanguageCode = "ja-JP",
            fallbackText = "[fallback]",
            timeout = kotlin.time.Duration.parse("5s"),
        )

        val transcription = runBlocking {
            transcriber.transcribe(
                SpeechTranscriptionCommand(
                    userId = java.util.UUID.randomUUID(),
                    audio = ByteArray(1) { 1 },
                    mimeType = "audio/wav",
                    languageCode = null,
                ),
            )
        }

        assertEquals("[fallback]", transcription.text)
        assertEquals("ja-JP", transcription.languageCode)
    }

    @Test
    fun `タイムアウト時もフォールバックを返す`() {
        val speechClient = mockk<SpeechClient>()
        every { speechClient.recognize(any<RecognitionConfig>(), any<RecognitionAudio>()) } answers {
            Thread.sleep(200)
            successResponse("too slow")
        }

        val transcriber = GoogleSpeechTranscriber(
            speechClient = speechClient,
            defaultLanguageCode = "ja-JP",
            fallbackText = "[fallback]",
            timeout = kotlin.time.Duration.parse("0.1s"),
        )

        val transcription = runBlocking {
            transcriber.transcribe(
                SpeechTranscriptionCommand(
                    userId = java.util.UUID.randomUUID(),
                    audio = ByteArray(1) { 1 },
                    mimeType = "audio/wav",
                    languageCode = null,
                ),
            )
        }

        assertEquals("[fallback]", transcription.text)
    }

    private fun successResponse(text: String): RecognizeResponse {
        val alternative = SpeechRecognitionAlternative.newBuilder()
            .setTranscript(text)
            .build()
        val result = SpeechRecognitionResult.newBuilder()
            .addAlternatives(alternative)
            .build()
        return RecognizeResponse.newBuilder()
            .addResults(result)
            .build()
    }
}
