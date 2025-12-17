package com.assari.voicebooklm.usecase.memo

import com.assari.voicebooklm.domain.model.VoiceMemo
import com.assari.voicebooklm.domain.repository.VoiceMemoRepository
import com.assari.voicebooklm.usecase.memo.client.AiMemoDraft
import com.assari.voicebooklm.usecase.memo.client.AiMemoFormatCommand
import com.assari.voicebooklm.usecase.memo.client.AiMemoFormatter
import com.assari.voicebooklm.usecase.memo.client.SpeechTranscriptionCommand
import com.assari.voicebooklm.usecase.memo.client.SpeechTranscriber
import com.assari.voicebooklm.usecase.support.ExecutionTimer
import com.assari.voicebooklm.usecase.support.MonotonicExecutionTimer
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
 * 音声文字起こしと AI 整形を経て VoiceMemo を生成するユースケース実装
 *
 * 文字起こしが失敗した場合は例外をスローし、AI整形は実行しない。
 * AI整形が失敗した場合のみフォールバックで進行する。
 */
open class CreateMemoInteractor(
    private val voiceMemoRepository: VoiceMemoRepository,
    private val speechTranscriber: SpeechTranscriber,
    private val aiMemoFormatter: AiMemoFormatter,
    private val executionTimer: ExecutionTimer = MonotonicExecutionTimer(),
    private val timeSource: TimeSource = TimeSource.Monotonic,
) : CreateMemoUseCase {

    private val logger = LoggerFactory.getLogger(CreateMemoInteractor::class.java)

    @Transactional
    override suspend fun execute(command: CreateMemoCommand): CreateMemoResult {
        require(command.audio.isNotEmpty()) { "Audio data must not be empty" }

        val overallMark = timeSource.markNow()
        val languageCode = command.language ?: "ja-JP"

        // 1. VoiceMemo を作成（処理待ち状態）
        var voiceMemo = VoiceMemo.create(
            userId = command.userId,
            languageCode = languageCode,
        )

        // 2. 文字起こし処理（失敗したら例外をスロー）
        voiceMemo = voiceMemo.startTranscription()
        val transcriptionResult = executionTimer.measure {
            runCatching {
                speechTranscriber.transcribe(
                    SpeechTranscriptionCommand(
                        userId = command.userId,
                        audio = command.audio,
                        mimeType = command.audioMimeType,
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
        val memoDraftResult = executionTimer.measure {
            runCatching {
                aiMemoFormatter.format(
                    AiMemoFormatCommand(
                        userId = command.userId,
                        transcript = transcriptionText,
                    ),
                )
            }.onFailure { ex ->
                logger.warn("AI memo formatting failed; fallback will be used", ex)
            }.getOrElse {
                fallbackDraftFromTranscript(transcriptionText)
            }
        }
        val memoDraft = memoDraftResult.value
        val formattingFallbackUsed = memoDraft.title == "ボイスメモ" && memoDraft.tags.isEmpty()
        voiceMemo = voiceMemo.completeFormatting(
            title = memoDraft.title,
            content = memoDraft.content,
            tags = memoDraft.tags,
            fallbackUsed = formattingFallbackUsed,
        )

        // 4. 永続化
        val (savedVoiceMemo, persistenceDuration) = executionTimer.measure {
            voiceMemoRepository.save(voiceMemo)
        }

        val totalDuration = overallMark.elapsedNow()

        return CreateMemoResult(
            voiceMemo = savedVoiceMemo,
            processingTime = ProcessingTime(
                transcription = transcriptionResult.duration,
                formatting = memoDraftResult.duration,
                persistence = persistenceDuration,
                total = totalDuration,
            ),
        )
    }

    private fun fallbackDraftFromTranscript(transcript: String) = AiMemoDraft(
        title = "ボイスメモ",
        content = transcript,
        tags = emptyList(),
    )
}
