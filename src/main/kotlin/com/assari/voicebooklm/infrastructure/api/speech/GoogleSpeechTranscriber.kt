package com.assari.voicebooklm.infrastructure.api.speech

import com.assari.voicebooklm.config.GoogleCloudProperties
import com.assari.voicebooklm.domain.gateway.SpeechTranscriber
import com.assari.voicebooklm.domain.gateway.SpeechTranscriptionCommand
import com.assari.voicebooklm.domain.gateway.SpeechTranscriptionResult
import com.assari.voicebooklm.infrastructure.api.storage.GcsStorageService
import com.google.cloud.speech.v1.RecognitionAudio
import com.google.cloud.speech.v1.RecognitionConfig
import com.google.cloud.speech.v1.SpeechClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Google Cloud Speech-to-Text クライアント実装（LongRunningRecognize 版）
 *
 * 音声ファイルを GCS にアップロードし、非同期の LongRunningRecognize API で
 * 文字起こしを行う。最大480分（8時間）の音声に対応。
 */
@Component
@Profile("!test")
class GoogleSpeechTranscriber(
    private val speechClient: SpeechClient,
    private val gcsStorageService: GcsStorageService,
    cloudProperties: GoogleCloudProperties,
) : SpeechTranscriber {
    private val defaultLanguageCode: String = cloudProperties.speech.defaultLanguage
    private val timeout: Duration = cloudProperties.speech.timeoutSeconds.seconds

    private val logger = LoggerFactory.getLogger(GoogleSpeechTranscriber::class.java)

    override suspend fun transcribe(command: SpeechTranscriptionCommand): SpeechTranscriptionResult {
        val languageCode = command.languageCode ?: defaultLanguageCode

        logger.info(
            "Starting transcription: audioSize={} bytes, languageCode={}",
            command.audio.size,
            languageCode
        )

        // 1. 音声を GCS にアップロード
        val uploadResult = gcsStorageService.uploadAudio(
            userId = command.userId,
            audioData = command.audio,
            mimeType = command.mimeType,
        )

        return try {
            withTimeout(timeout) {
                withContext(Dispatchers.IO) {
                    // 2. 認識設定を構築
                    val config = buildRecognitionConfig(languageCode, command.mimeType)

                    // 3. GCS URI を使用して認識リクエストを作成
                    val audio = RecognitionAudio.newBuilder()
                        .setUri(uploadResult.gcsUri)
                        .build()

                    logger.info("Starting LongRunningRecognize: gcsUri={}", uploadResult.gcsUri)

                    // 4. 非同期認識を開始（LongRunningRecognize）
                    val operationFuture = speechClient.longRunningRecognizeAsync(config, audio)

                    // 5. 完了を待機
                    val response = operationFuture.get(timeout.inWholeSeconds, TimeUnit.SECONDS)

                    logger.info("Speech API response: resultsCount={}", response.resultsCount)

                    // 6. 結果を抽出
                    val text = response.resultsList.joinToString("\n") { result ->
                        result.alternativesList.firstOrNull()?.transcript.orEmpty()
                    }.trim()

                    if (text.isBlank()) {
                        logger.warn("Speech API returned empty results")
                    } else {
                        logger.info("Transcription successful: textLength={}", text.length)
                    }

                    SpeechTranscriptionResult(
                        text = text,
                        languageCode = languageCode,
                    )
                }
            }
        } catch (ex: Exception) {
            when {
                ex is CancellationException && ex !is TimeoutCancellationException -> throw ex
                else -> {
                    logger.error("Speech transcription failed: {}", ex.message, ex)
                    throw ex
                }
            }
        } finally {
            // 7. GCS から音声ファイルを削除（クリーンアップ）
            try {
                gcsStorageService.deleteAudio(uploadResult.objectName)
            } catch (cleanupEx: Exception) {
                logger.warn("Failed to cleanup GCS audio file: {}", cleanupEx.message)
            }
        }
    }

    /**
     * MIME タイプに基づいて RecognitionConfig を構築
     *
     * エンコーディングは指定するが、サンプルレートは自動検出に任せる。
     */
    private fun buildRecognitionConfig(languageCode: String, mimeType: String): RecognitionConfig {
        val builder = RecognitionConfig.newBuilder()
            .setLanguageCode(languageCode)
            .setEnableAutomaticPunctuation(true)

        // エンコーディングは指定、サンプルレートは自動検出
        when {
            mimeType.contains("wav") || mimeType.contains("wave") -> {
                builder.setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                // サンプルレートは指定しない（自動検出）
            }
            mimeType.contains("flac") -> {
                builder.setEncoding(RecognitionConfig.AudioEncoding.FLAC)
            }
            mimeType.contains("mp3") || mimeType.contains("mpeg") -> {
                builder.setEncoding(RecognitionConfig.AudioEncoding.MP3)
            }
            mimeType.contains("ogg") -> {
                builder.setEncoding(RecognitionConfig.AudioEncoding.OGG_OPUS)
            }
            mimeType.contains("webm") -> {
                builder.setEncoding(RecognitionConfig.AudioEncoding.WEBM_OPUS)
            }
            else -> {
                // その他は自動検出
                builder.setEncoding(RecognitionConfig.AudioEncoding.ENCODING_UNSPECIFIED)
            }
        }

        logger.debug("RecognitionConfig: languageCode={}, mimeType={}, encoding={}",
            languageCode, mimeType, builder.encoding)

        return builder.build()
    }
}
