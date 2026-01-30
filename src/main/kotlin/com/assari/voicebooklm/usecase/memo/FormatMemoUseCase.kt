package com.assari.voicebooklm.usecase.memo

import com.assari.voicebooklm.domain.exception.DomainException
import com.assari.voicebooklm.domain.exception.ErrorCode
import com.assari.voicebooklm.domain.gateway.MemoFormatCommand
import com.assari.voicebooklm.domain.gateway.MemoFormatResult
import com.assari.voicebooklm.domain.gateway.MemoFormatter
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
 * 文字起こしテキストを受け取り、AI整形してメモを保存するユースケース
 *
 * WebSocketで受け取った文字起こしテキストを整形して保存する。
 * 既存のCreateMemoUseCaseから文字起こし部分を除いた簡略版。
 */
@Service
open class FormatMemoUseCase(
    private val voiceMemoRepository: VoiceMemoRepository,
    private val memoFormatter: MemoFormatter,
    private val folderRepository: FolderRepository,
    private val folderPathResolver: FolderPathResolver,
    private val executionTimer: ExecutionTimer = MonotonicExecutionTimer(),
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    private val logger = LoggerFactory.getLogger(FormatMemoUseCase::class.java)

    @Transactional
    open suspend fun execute(input: FormatMemoInput): FormatMemoOutput {
        require(input.transcription.isNotBlank()) { "Transcription text must not be empty" }

        // folderId 指定時は AI 呼び出し前にフォルダの存在・所有を検証する
        input.folderId?.let { folderId ->
            val folder = folderRepository.findById(folderId)
                ?: throw DomainException(ErrorCode.FOLDER_NOT_FOUND, "フォルダーが見つかりません: $folderId")
            if (folder.userId != input.userId) {
                throw DomainException(ErrorCode.FOLDER_NOT_FOUND, "フォルダーが見つかりません: $folderId")
            }
        }

        val overallMark = timeSource.markNow()
        val languageCode = input.language ?: "ja-JP"

        // 1. VoiceMemo を作成（文字起こし完了状態で作成）
        var voiceMemo = VoiceMemo.create(
            id = UuidCreator.getTimeOrderedEpoch(),
            userId = input.userId,
            languageCode = languageCode,
        )
        voiceMemo = voiceMemo.startTranscription()
        voiceMemo = voiceMemo.completeTranscription(
            text = input.transcription,
            fallbackUsed = false,
        )

        // 2. 既存フォルダーパスを取得（AI整形用）
        val existingFolderPaths = getExistingFolderPaths(input.userId)
        val trimmedFolderPath = input.folderPath?.trim()

        // 3. AI整形処理（失敗した場合はフォールバックで続行）
        val folderSpecifiedByUser = input.folderId != null || trimmedFolderPath?.isNotBlank() == true
        val userSpecifiedTags = input.tags?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
        voiceMemo = voiceMemo.startFormatting()
        val formatResult = executionTimer.measure {
            runCatching {
                memoFormatter.format(
                    MemoFormatCommand(
                        userId = input.userId,
                        transcript = input.transcription,
                        existingFolderPaths = existingFolderPaths,
                        folderSpecifiedByUser = folderSpecifiedByUser,
                        userSpecifiedTags = userSpecifiedTags,
                    ),
                )
            }.onFailure { ex ->
                logger.warn("AI memo formatting failed; fallback will be used", ex)
            }.getOrElse {
                fallbackFormatResult(input.transcription)
            }
        }
        val memoFormat = formatResult.value

        // 4. フォルダIDの決定（ユーザー指定優先、なければAIの結果を解決）
        val effectiveFolderId = when {
            input.folderId != null -> input.folderId
            trimmedFolderPath?.isNotBlank() == true ->
                resolveFolderId(input.userId, trimmedFolderPath)
            else -> resolveFolderId(input.userId, memoFormat.folderPath)
        }

        // 5. タグのマージ（ユーザー指定 + AI提案、重複は小文字で除去）
        // 大文字小文字のみ異なるタグがある場合は最初に出現したものを保持するため、
        // ユーザー指定タグを先に結合している（ユーザー指定タグの表記を優先する）
        val effectiveTags = (userSpecifiedTags + memoFormat.tags).distinctBy { it.lowercase() }

        val formattingFallbackUsed = memoFormat.title == "ボイスメモ" && memoFormat.tags.isEmpty()
        voiceMemo = voiceMemo.completeFormatting(
            title = memoFormat.title,
            content = memoFormat.content,
            tags = effectiveTags,
            fallbackUsed = formattingFallbackUsed,
            folderId = effectiveFolderId,
        )

        // 6. 永続化
        val (savedVoiceMemo, persistenceDuration) = executionTimer.measure {
            voiceMemoRepository.save(voiceMemo)
        }

        val totalDuration = overallMark.elapsedNow()

        logger.info(
            "memo formatted and saved memoId={} userId={} totalMs={} fallbackF={}",
            savedVoiceMemo.id,
            input.userId,
            totalDuration.inWholeMilliseconds,
            savedVoiceMemo.formatting.fallbackUsed,
        )

        return FormatMemoOutput(
            voiceMemo = savedVoiceMemo,
            processingTime = FormatProcessingTime(
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
 * 整形入力
 */
data class FormatMemoInput(
    val userId: UUID,
    /** 文字起こしテキスト（WebSocketで受信済み） */
    val transcription: String,
    /** 言語コード（例: ja-JP） */
    val language: String? = null,
    /** 保存先フォルダーID。指定時はAIのフォルダ分類は行わない。folderPath より優先される */
    val folderId: UUID? = null,
    /**
     * 保存先のフォルダーパス。
     * 指定時は [FolderPathResolver.resolveOrCreate] で解決し、
     * 指定されたパスのフォルダが存在しない場合は最大3階層まで自動作成する。
     * 4階層以上のパスを指定した場合もバリデーションエラーにはならず、
     * 3階層目までのフォルダが作成され、そのリーフが保存先となる。
     * [folderId] が指定されている場合は無視される。
     */
    val folderPath: String? = null,
    /** 録音時に付けたいタグ。AI提案タグとマージして保存する */
    val tags: List<String>? = null,
)

/**
 * 整形出力
 */
data class FormatMemoOutput(
    val voiceMemo: VoiceMemo,
    val processingTime: FormatProcessingTime,
)

/**
 * 処理時間メトリクス
 */
data class FormatProcessingTime(
    val formatting: Duration,
    val persistence: Duration,
    val total: Duration,
)
