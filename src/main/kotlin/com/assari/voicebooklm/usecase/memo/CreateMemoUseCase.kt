package com.assari.voicebooklm.usecase.memo

import com.assari.voicebooklm.domain.model.VoiceMemo
import java.util.UUID
import kotlin.time.Duration

/**
 * 音声入力からメモを生成するユースケース
 */
interface CreateMemoUseCase {
    suspend fun execute(command: CreateMemoCommand): CreateMemoResult
}

/**
 * メモ生成要求
 */
data class CreateMemoCommand(
    val userId: UUID,
    val audio: ByteArray,
    val audioMimeType: String,
    val language: String? = null,
)

/**
 * メモ生成結果
 */
data class CreateMemoResult(
    val voiceMemo: VoiceMemo,
    val processingTime: ProcessingTime,
)

/**
 * 各工程の処理時間メトリクス
 */
data class ProcessingTime(
    val transcription: Duration,
    val formatting: Duration,
    val persistence: Duration,
    val total: Duration,
)
