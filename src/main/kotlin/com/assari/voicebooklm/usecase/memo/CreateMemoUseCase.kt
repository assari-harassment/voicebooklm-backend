package com.assari.voicebooklm.usecase.memo

import com.assari.voicebooklm.domain.gateway.MemoFormatCommand
import com.assari.voicebooklm.domain.gateway.MemoFormatResult
import com.assari.voicebooklm.domain.gateway.MemoFormatter
import com.assari.voicebooklm.domain.gateway.SpeechTranscriber
import com.assari.voicebooklm.domain.gateway.SpeechTranscriptionCommand
import com.assari.voicebooklm.domain.model.VoiceMemo
import com.assari.voicebooklm.domain.repository.VoiceMemoRepository
import com.assari.voicebooklm.usecase.support.ExecutionTimer
import com.assari.voicebooklm.usecase.support.MonotonicExecutionTimer
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.TimeSource
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional

/**
 * 文字起こし失敗時の例外
 */
class TranscriptionFailedException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/**
 * 音声文字起こしと AI 整形を経て VoiceMemo を生成するユースケース
 *
 * 文字起こしが失敗した場合は例外をスローし、AI整形は実行しない。
 * AI整形が失敗した場合のみフォールバックで進行する。
 */
open class CreateMemoUseCase(
    private val voiceMemoRepository: VoiceMemoRepository,
    private val speechTranscriber: SpeechTranscriber,
    private val memoFormatter: MemoFormatter,
    private val executionTimer: ExecutionTimer = MonotonicExecutionTimer(),
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    private val logger = LoggerFactory.getLogger(CreateMemoUseCase::class.java)

    @Transactional
    open suspend fun execute(input: CreateMemoInput): CreateMemoOutput {
        require(input.audio.isNotEmpty()) { "Audio data must not be empty" }

        val overallMark = timeSource.markNow()
        val languageCode = input.language ?: "ja-JP"

        // 1. VoiceMemo を作成（処理待ち状態）
        var voiceMemo = VoiceMemo.create(
            userId = input.userId,
            languageCode = languageCode,
        )

        // 2. 文字起こし処理（失敗したら例外をスロー）
        voiceMemo = voiceMemo.startTranscription()
        val transcriptionResult = executionTimer.measure {
            runCatching {
                speechTranscriber.transcribe(
                    SpeechTranscriptionCommand(
                        userId = input.userId,
                        audio = input.audio,
                        mimeType = input.audioMimeType,
                        languageCode = languageCode,
                    ),
                )
            }.getOrElse { ex ->
                logger.error("Speech transcription failed", ex)
                // 失敗状態で保存して例外をスロー
                voiceMemo = voiceMemo.failTranscription()
                voiceMemoRepository.save(voiceMemo)
                throw TranscriptionFailedException("文字起こしに失敗しました", ex)
            }
        }

        val transcriptionText = transcriptionResult.value.text
        if (transcriptionText.isBlank()) {
            logger.warn("Speech transcription returned empty result")
            voiceMemo = voiceMemo.failTranscription()
            voiceMemoRepository.save(voiceMemo)
            throw TranscriptionFailedException("文字起こし結果が空でした。音声が認識できなかった可能性があります。")
        }

        voiceMemo = voiceMemo.completeTranscription(
            text = transcriptionText,
            fallbackUsed = false,
        )

        // 3. AI整形処理（失敗した場合はフォールバックで続行）
        voiceMemo = voiceMemo.startFormatting()
        val formatResult = executionTimer.measure {
            runCatching {
                memoFormatter.format(
                    MemoFormatCommand(
                        userId = input.userId,
                        transcript = transcriptionText,
                    ),
                )
            }.onFailure { ex ->
                logger.warn("AI memo formatting failed; fallback will be used", ex)
            }.getOrElse {
                fallbackFormatResult(transcriptionText)
            }
        }
        val memoFormat = formatResult.value
        val formattingFallbackUsed = memoFormat.title == "ボイスメモ" && memoFormat.tags.isEmpty()
        voiceMemo = voiceMemo.completeFormatting(
            title = memoFormat.title,
            content = memoFormat.content,
            tags = memoFormat.tags,
            fallbackUsed = formattingFallbackUsed,
        )

        // 4. 永続化
        val (savedVoiceMemo, persistenceDuration) = executionTimer.measure {
            voiceMemoRepository.save(voiceMemo)
        }

        val totalDuration = overallMark.elapsedNow()

        return CreateMemoOutput(
            voiceMemo = savedVoiceMemo,
            processingTime = ProcessingTime(
                transcription = transcriptionResult.duration,
                formatting = formatResult.duration,
                persistence = persistenceDuration,
                total = totalDuration,
            ),
        )
    }

    private fun fallbackFormatResult(transcript: String) = MemoFormatResult(
        title = "ボイスメモ",
        content = transcript,
        tags = emptyList(),
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// Input / Output
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * メモ生成Input
 */
data class CreateMemoInput(
    val userId: UUID,
    val audio: ByteArray,
    val audioMimeType: String,
    val language: String? = null,
)

/**
 * メモ生成Output
 */
data class CreateMemoOutput(
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
