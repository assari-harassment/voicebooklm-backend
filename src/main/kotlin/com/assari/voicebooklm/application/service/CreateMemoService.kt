package com.assari.voicebooklm.application.service

import com.assari.voicebooklm.application.port.`in`.CreateMemoCommand
import com.assari.voicebooklm.application.port.`in`.CreateMemoResult
import com.assari.voicebooklm.application.port.`in`.CreateMemoUseCase
import com.assari.voicebooklm.application.port.`in`.ProcessingTime
import com.assari.voicebooklm.application.port.out.AiMemoFormatCommand
import com.assari.voicebooklm.application.port.out.AiMemoFormatter
import com.assari.voicebooklm.application.port.out.SpeechTranscriptionCommand
import com.assari.voicebooklm.application.port.out.SpeechTranscriber
import com.assari.voicebooklm.application.support.ExecutionTimer
import com.assari.voicebooklm.application.support.MonotonicExecutionTimer
import com.assari.voicebooklm.domain.memo.Memo
import com.assari.voicebooklm.domain.memo.MemoRepository
import kotlin.time.TimeSource

/**
 * 音声文字起こしと AI 整形を経てメモを生成するユースケース実装。
 */
class CreateMemoService(
    private val memoRepository: MemoRepository,
    private val speechTranscriber: SpeechTranscriber,
    private val aiMemoFormatter: AiMemoFormatter,
    private val executionTimer: ExecutionTimer = MonotonicExecutionTimer(),
    private val timeSource: TimeSource = TimeSource.Monotonic,
) : CreateMemoUseCase {

    override suspend fun execute(command: CreateMemoCommand): CreateMemoResult {
        require(command.audio.isNotEmpty()) { "Audio data must not be empty" }

        val overallMark = timeSource.markNow()

        val (transcription, transcriptionDuration) = executionTimer.measure {
            speechTranscriber.transcribe(
                SpeechTranscriptionCommand(
                    userId = command.userId,
                    audio = command.audio,
                    mimeType = command.audioMimeType,
                    languageCode = command.language,
                ),
            )
        }
        require(transcription.text.isNotBlank()) { "Transcription result must not be blank" }

        val (memoDraft, formattingDuration) = executionTimer.measure {
            aiMemoFormatter.format(
                AiMemoFormatCommand(
                    userId = command.userId,
                    transcript = transcription.text,
                ),
            )
        }

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
                transcription = transcriptionDuration,
                formatting = formattingDuration,
                persistence = persistenceDuration,
                total = totalDuration,
            ),
        )
    }
}
