package com.assari.voicebooklm.usecase.memo

import com.assari.voicebooklm.domain.exception.DomainException
import com.assari.voicebooklm.domain.exception.ErrorCode
import com.assari.voicebooklm.domain.gateway.MemoFormatCommand
import com.assari.voicebooklm.domain.gateway.MemoFormatResult
import com.assari.voicebooklm.domain.gateway.MemoFormatter
import com.assari.voicebooklm.domain.gateway.SpeechTranscriber
import com.assari.voicebooklm.domain.gateway.SpeechTranscriptionCommand
import com.assari.voicebooklm.domain.model.Folder
import com.assari.voicebooklm.domain.model.VoiceMemo
import com.assari.voicebooklm.domain.model.buildPath
import com.assari.voicebooklm.domain.repository.FolderRepository
import com.assari.voicebooklm.domain.repository.VoiceMemoRepository
import com.assari.voicebooklm.infrastructure.service.FolderPathResolver
import com.assari.voicebooklm.usecase.support.ExecutionTimer
import com.assari.voicebooklm.usecase.support.MonotonicExecutionTimer
import com.github.f4b6a3.uuid.UuidCreator
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.TimeSource
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 音声文字起こしと AI 整形を経て VoiceMemo を生成するユースケース
 *
 * 文字起こしが失敗した場合は例外をスローし、AI整形は実行しない。
 * AI整形が失敗した場合のみフォールバックで進行する。
 */
@Service
open class CreateMemoUseCase(
    private val voiceMemoRepository: VoiceMemoRepository,
    private val speechTranscriber: SpeechTranscriber,
    private val memoFormatter: MemoFormatter,
    private val folderRepository: FolderRepository,
    private val folderPathResolver: FolderPathResolver,
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
            id = UuidCreator.getTimeOrderedEpoch(),
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
                throw DomainException(ErrorCode.TRANSCRIPTION_FAILED, "文字起こしに失敗しました", ex)
            }
        }

        val transcriptionText = transcriptionResult.value.text
        if (transcriptionText.isBlank()) {
            logger.warn("Speech transcription returned empty result")
            voiceMemo = voiceMemo.failTranscription()
            voiceMemoRepository.save(voiceMemo)
            throw DomainException(ErrorCode.TRANSCRIPTION_FAILED, "文字起こし結果が空でした。音声が認識できなかった可能性があります。")
        }

        voiceMemo = voiceMemo.completeTranscription(
            text = transcriptionText,
            fallbackUsed = false,
        )

        // 3. 既存フォルダーパスを取得（AI整形用）
        val existingFolderPaths = getExistingFolderPaths(input.userId)

        // 4. AI整形処理（失敗した場合はフォールバックで続行）
        voiceMemo = voiceMemo.startFormatting()
        val formatResult = executionTimer.measure {
            runCatching {
                memoFormatter.format(
                    MemoFormatCommand(
                        userId = input.userId,
                        transcript = transcriptionText,
                        existingFolderPaths = existingFolderPaths,
                    ),
                )
            }.onFailure { ex ->
                logger.warn("AI memo formatting failed; fallback will be used", ex)
            }.getOrElse {
                fallbackFormatResult(transcriptionText)
            }
        }
        val memoFormat = formatResult.value

        // 5. フォルダーパスをIDに解決（必要に応じて作成）
        val folderId = resolveFolderId(input.userId, memoFormat.folderPath)

        val formattingFallbackUsed = memoFormat.title == "ボイスメモ" && memoFormat.tags.isEmpty()
        voiceMemo = voiceMemo.completeFormatting(
            title = memoFormat.title,
            content = memoFormat.content,
            tags = memoFormat.tags,
            fallbackUsed = formattingFallbackUsed,
            folderId = folderId,
        )

        // 6. 永続化
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
        folderPath = null,
    )

    /**
     * ユーザーの既存フォルダーパス一覧を取得する
     */
    private suspend fun getExistingFolderPaths(userId: UUID): List<String> {
        val folders = folderRepository.findByUserId(userId)
        if (folders.isEmpty()) return emptyList()

        val folderMap = folders.associateBy { it.id }
        return folders.map { folder -> folder.buildPath(folderMap) }.sorted()
    }

    /**
     * フォルダーパスをIDに解決する（必要に応じて作成）
     */
    private suspend fun resolveFolderId(userId: UUID, folderPath: String?): UUID? {
        if (folderPath.isNullOrBlank()) return null

        return runCatching {
            folderPathResolver.resolveOrCreate(userId, folderPath)
        }.onFailure { ex ->
            logger.warn("Failed to resolve folder path: $folderPath", ex)
        }.getOrNull()
    }
}

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
