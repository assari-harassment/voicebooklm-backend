package com.assari.voicebooklm.application.port.`in`

import com.assari.voicebooklm.application.port.out.SpeechTranscription
import com.assari.voicebooklm.domain.memo.Memo
import java.util.UUID
import kotlin.time.Duration

/**
 * 音声入力からメモを生成するユースケース。
 */
interface CreateMemoUseCase {
    suspend fun execute(command: CreateMemoCommand): CreateMemoResult
}

/**
 * メモ生成要求。
 */
data class CreateMemoCommand(
    val userId: UUID,
    val audio: ByteArray,
    val audioMimeType: String,
    val language: String? = null,
)

/**
 * メモ生成結果。
 */
data class CreateMemoResult(
    val memo: Memo,
    val transcription: SpeechTranscription,
    val processingTime: ProcessingTime,
)

/**
 * 各工程の処理時間メトリクス。
 */
data class ProcessingTime(
    val transcription: Duration,
    val formatting: Duration,
    val persistence: Duration,
    val total: Duration,
)
