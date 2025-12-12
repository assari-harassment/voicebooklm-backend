package com.assari.voicebooklm.infrastructure.api.speech

import com.assari.voicebooklm.usecase.memo.port.SpeechTranscriber
import com.assari.voicebooklm.usecase.memo.port.SpeechTranscription
import com.assari.voicebooklm.usecase.memo.port.SpeechTranscriptionCommand
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

    override suspend fun transcribe(command: SpeechTranscriptionCommand): SpeechTranscription =
        runCatching {
            withTimeout(timeout) {
                withContext(Dispatchers.IO) {
                    val languageCode = command.languageCode ?: defaultLanguageCode
                    val config = RecognitionConfig.newBuilder()
                        .setLanguageCode(languageCode)
                        .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                        .build()
                    val audio = RecognitionAudio.newBuilder()
                        .setContent(ByteString.copyFrom(command.audio))
                        .build()

                    val response = speechClient.recognize(config, audio)
                    val text = response.resultsList.joinToString("\n") { result ->
                        result.alternativesList.firstOrNull()?.transcript.orEmpty()
                    }.trim()

                    SpeechTranscription(
                        text = text.ifBlank { fallbackText },
                        languageCode = languageCode,
                    )
                }
            }
        }.getOrElse { ex ->
            if (ex is CancellationException && ex !is TimeoutCancellationException) throw ex
            val languageCode = command.languageCode ?: defaultLanguageCode
            SpeechTranscription(
                text = fallbackText,
                languageCode = languageCode,
            )
        }
}
