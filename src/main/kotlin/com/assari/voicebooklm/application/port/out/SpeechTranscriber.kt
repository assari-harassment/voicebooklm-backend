package com.assari.voicebooklm.application.port.out

import java.util.UUID

/**
 * 音声をテキストへ変換するポート。
 */
interface SpeechTranscriber {
    suspend fun transcribe(command: SpeechTranscriptionCommand): SpeechTranscription
}

/**
 * 文字起こし要求。
 */
data class SpeechTranscriptionCommand(
    val userId: UUID,
    val audio: ByteArray,
    val mimeType: String,
    val languageCode: String?,
)

/**
 * 文字起こし結果。
 */
data class SpeechTranscription(
    val text: String,
    val languageCode: String?,
)
