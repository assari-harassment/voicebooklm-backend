package com.assari.voicebooklm.infrastructure.api.speech

import com.assari.voicebooklm.usecase.memo.client.SpeechTranscriber
import com.assari.voicebooklm.usecase.memo.client.SpeechTranscription
import com.assari.voicebooklm.usecase.memo.client.SpeechTranscriptionCommand
import com.google.cloud.speech.v1.RecognitionAudio
import com.google.cloud.speech.v1.RecognitionConfig
import com.google.cloud.speech.v1.SpeechClient
import com.google.protobuf.ByteString
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import org.slf4j.LoggerFactory

/**
 * Google Cloud Speech-to-Text クライアント実装。
 * 60 秒以内の同期文字起こしを行い、失敗時はフォールバックテキストを返す。
 */
class GoogleSpeechTranscriber(
    private val speechClient: SpeechClient,
    private val defaultLanguageCode: String = "ja-JP",
    private val fallbackText: String = "[transcription unavailable]",
    private val timeout: Duration = 60.seconds,
) : SpeechTranscriber {

    private val logger = LoggerFactory.getLogger(GoogleSpeechTranscriber::class.java)

    companion object {
        // Google Speech API 同期認識の最大サイズ (10MB)
        private const val MAX_AUDIO_SIZE_BYTES = 10 * 1024 * 1024
    }

    override suspend fun transcribe(command: SpeechTranscriptionCommand): SpeechTranscription =
        runCatching {
            withTimeout(timeout) {
                withContext(Dispatchers.IO) {
                    val languageCode = command.languageCode ?: defaultLanguageCode
                    logger.info("Starting transcription: audioSize={} bytes, languageCode={}", command.audio.size, languageCode)

                    // ファイルサイズチェック
                    if (command.audio.size > MAX_AUDIO_SIZE_BYTES) {
                        logger.warn(
                            "Audio file too large: {} bytes (max: {} bytes). Consider using shorter audio.",
                            command.audio.size,
                            MAX_AUDIO_SIZE_BYTES
                        )
                        throw IllegalArgumentException(
                            "音声ファイルが大きすぎます（最大10MB）。短い音声を使用してください。"
                        )
                    }

                    val config = RecognitionConfig.newBuilder()
                        .setLanguageCode(languageCode)
                        .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                        .setSampleRateHertz(16000) // 16kHz を明示的に指定
                        .build()
                    val audio = RecognitionAudio.newBuilder()
                        .setContent(ByteString.copyFrom(command.audio))
                        .build()

                    val response = speechClient.recognize(config, audio)
                    logger.info("Speech API response: resultsCount={}", response.resultsCount)

                    val text = response.resultsList.joinToString("\n") { result ->
                        result.alternativesList.firstOrNull()?.transcript.orEmpty()
                    }.trim()

                    if (text.isBlank()) {
                        logger.warn("Speech API returned empty results, using fallback")
                    } else {
                        logger.info("Transcription successful: textLength={}", text.length)
                    }

                    SpeechTranscription(
                        text = text.ifBlank { fallbackText },
                        languageCode = languageCode,
                    )
                }
            }
        }.getOrElse { ex ->
            logger.error("Speech transcription failed: {}", ex.message, ex)
            if (ex is CancellationException && ex !is TimeoutCancellationException) throw ex
            val languageCode = command.languageCode ?: defaultLanguageCode
            SpeechTranscription(
                text = fallbackText,
                languageCode = languageCode,
            )
        }
}
