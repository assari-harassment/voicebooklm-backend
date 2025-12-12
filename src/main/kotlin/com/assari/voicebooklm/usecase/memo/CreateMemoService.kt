package com.assari.voicebooklm.usecase.memo

import com.assari.voicebooklm.domain.model.Memo
import com.assari.voicebooklm.domain.repository.MemoRepository
import com.assari.voicebooklm.usecase.memo.client.AiMemoDraft
import com.assari.voicebooklm.usecase.memo.client.AiMemoFormatCommand
import com.assari.voicebooklm.usecase.memo.client.AiMemoFormatter
import com.assari.voicebooklm.usecase.memo.client.SpeechTranscription
import com.assari.voicebooklm.usecase.memo.client.SpeechTranscriptionCommand
import com.assari.voicebooklm.usecase.memo.client.SpeechTranscriber
import com.assari.voicebooklm.usecase.support.ExecutionTimer
import com.assari.voicebooklm.usecase.support.MonotonicExecutionTimer
import kotlin.time.TimeSource
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional

/**
 * 音声文字起こしと AI 整形を経てメモを生成するユースケース実装。
 * 各工程のタイムアウト/例外時はフォールバックで進行し、警告ログを出力する。
 */
class CreateMemoService(
    private val memoRepository: MemoRepository,
    private val speechTranscriber: SpeechTranscriber,
    private val aiMemoFormatter: AiMemoFormatter,
    private val executionTimer: ExecutionTimer = MonotonicExecutionTimer(),
    private val timeSource: TimeSource = TimeSource.Monotonic,
) : CreateMemoUseCase {

    private val logger = LoggerFactory.getLogger(CreateMemoService::class.java)
    private val defaultTranscriptFallback = "[transcription unavailable]"

    @Transactional
    override suspend fun execute(command: CreateMemoCommand): CreateMemoResult {
        require(command.audio.isNotEmpty()) { "Audio data must not be empty" }

        val overallMark = timeSource.markNow()

        val transcriptionResult = executionTimer.measure {
            runCatching {
                speechTranscriber.transcribe(
                    SpeechTranscriptionCommand(
                        userId = command.userId,
                        audio = command.audio,
                        mimeType = command.audioMimeType,
                        languageCode = command.language,
                    ),
                )
            }.onFailure { ex ->
                logger.warn("Speech transcription failed; fallback will be used", ex)
            }.getOrDefault(
                SpeechTranscription(
                    text = defaultTranscriptFallback,
                    languageCode = command.language,
                ),
            )
        }
        val safeTranscriptionText = transcriptionResult.value.text.ifBlank { defaultTranscriptFallback }
        val transcriptionFallbackUsed = safeTranscriptionText == defaultTranscriptFallback
        val transcription = transcriptionResult.value.copy(text = safeTranscriptionText)

        val memoDraftResult = executionTimer.measure {
            runCatching {
                aiMemoFormatter.format(
                    AiMemoFormatCommand(
                        userId = command.userId,
                        transcript = transcription.text,
                    ),
                )
            }.onFailure { ex ->
                logger.warn("AI memo formatting failed; fallback will be used", ex)
            }.getOrElse {
                fallbackDraftFromTranscript(transcription.text)
            }
        }
        val formattingFallbackUsed = memoDraftResult.value.title == "ボイスメモ" && memoDraftResult.value.tags.isEmpty()
        val memoDraft = memoDraftResult.value

        val memoToSave = Memo.create(
            title = memoDraft.title,
            content = memoDraft.content,
            tags = memoDraft.tags,
            userId = command.userId,
        )

        val (savedMemo, persistenceDuration) = executionTimer.measure {
            memoRepository.save(memoToSave)
        }

        val totalDuration = overallMark.elapsedNow()

        return CreateMemoResult(
            memo = savedMemo,
            transcription = transcription,
            processingTime = ProcessingTime(
                transcription = transcriptionResult.duration,
                formatting = memoDraftResult.duration,
                persistence = persistenceDuration,
                total = totalDuration,
            ),
            fallbackUsage = FallbackUsage(
                transcription = transcriptionFallbackUsed,
                formatting = formattingFallbackUsed,
            ),
        )
    }

    private fun fallbackDraftFromTranscript(transcript: String) = AiMemoDraft(
        title = "ボイスメモ",
        content = transcript.ifBlank { "音声内容を取得できませんでした。" },
        tags = emptyList(),
    )
}
