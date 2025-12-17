package com.assari.voicebooklm.domain.gateway

import java.util.UUID

/**
 * 音声をテキストへ変換するゲートウェイ
 */
interface SpeechTranscriber {
    suspend fun transcribe(command: SpeechTranscriptionCommand): SpeechTranscriptionResult
}

/**
 * 文字起こし要求
 */
data class SpeechTranscriptionCommand(
    val userId: UUID,
    val audio: ByteArray,
    val mimeType: String,
    val languageCode: String?,
)

/**
 * 文字起こし結果
 */
data class SpeechTranscriptionResult(
    val text: String,
    val languageCode: String?,
)
