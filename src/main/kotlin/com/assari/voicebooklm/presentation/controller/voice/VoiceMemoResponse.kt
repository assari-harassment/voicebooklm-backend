package com.assari.voicebooklm.presentation.controller.voice

import com.assari.voicebooklm.usecase.memo.CreateMemoOutput
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.toJavaDuration

/**
 * 音声メモ作成レスポンス
 */
data class VoiceMemoCreatedResponse(
    val memoId: UUID,
    val title: String,
    val content: String,
    val tagIds: List<UUID>,
    val transcription: String?,
    val transcriptionStatus: String,
    val formattingStatus: String,
    val processingTimeMillis: ProcessingTimeResponse,
    val fallback: FallbackUsageResponse,
) {
    companion object {
        fun from(result: CreateMemoOutput): VoiceMemoCreatedResponse {
            val voiceMemo = result.voiceMemo
            return VoiceMemoCreatedResponse(
                memoId = voiceMemo.id,
                title = voiceMemo.title ?: "",
                content = voiceMemo.content ?: "",
                tagIds = voiceMemo.tagIds,
                transcription = voiceMemo.transcriptionText,
                transcriptionStatus = voiceMemo.transcription.status.name,
                formattingStatus = voiceMemo.formatting.status.name,
                processingTimeMillis = ProcessingTimeResponse(
                    transcription = result.processingTime.transcription.toMillis(),
                    formatting = result.processingTime.formatting.toMillis(),
                    persistence = result.processingTime.persistence.toMillis(),
                    total = result.processingTime.total.toMillis(),
                ),
                fallback = FallbackUsageResponse(
                    transcription = voiceMemo.transcription.fallbackUsed,
                    formatting = voiceMemo.formatting.fallbackUsed,
                ),
            )
        }
    }
}

/**
 * 処理時間レスポンス
 */
data class ProcessingTimeResponse(
    val transcription: Long,
    val formatting: Long,
    val persistence: Long,
    val total: Long,
)

/**
 * フォールバック使用状況レスポンス
 */
data class FallbackUsageResponse(
    val transcription: Boolean,
    val formatting: Boolean,
)

private fun Duration.toMillis(): Long = this.toJavaDuration().toMillis()
