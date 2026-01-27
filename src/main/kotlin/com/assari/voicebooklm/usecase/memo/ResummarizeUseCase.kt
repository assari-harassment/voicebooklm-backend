package com.assari.voicebooklm.usecase.memo

import com.assari.voicebooklm.domain.exception.DomainException
import com.assari.voicebooklm.domain.exception.ErrorCode
import com.assari.voicebooklm.domain.gateway.MemoFormatCommand
import com.assari.voicebooklm.domain.gateway.MemoFormatResult
import com.assari.voicebooklm.domain.gateway.MemoFormatter
import com.assari.voicebooklm.domain.model.VoiceMemo
import com.assari.voicebooklm.domain.model.buildPath
import com.assari.voicebooklm.domain.repository.FolderRepository
import com.assari.voicebooklm.domain.repository.VoiceMemoRepository
import com.assari.voicebooklm.infrastructure.service.FolderPathResolver
import com.assari.voicebooklm.usecase.support.ExecutionTimer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.time.Duration

/**
 * 編集された文字起こしテキストから再要約するユースケース
 */
@Service
open class ResummarizeUseCase(
    private val voiceMemoRepository: VoiceMemoRepository,
    private val folderRepository: FolderRepository,
    private val folderPathResolver: FolderPathResolver,
    private val memoFormatter: MemoFormatter,
    private val executionTimer: ExecutionTimer,
) {
    private val logger = LoggerFactory.getLogger(ResummarizeUseCase::class.java)

    @Transactional
    open suspend fun execute(input: ResummarizeInput): ResummarizeOutput {
        // 1. メモの取得と権限チェック
        val voiceMemo = voiceMemoRepository.findById(input.memoId)
            ?: throw DomainException(ErrorCode.MEMO_NOT_FOUND)

        if (voiceMemo.userId != input.userId) {
            throw DomainException(ErrorCode.MEMO_NOT_FOUND)
        }

        if (voiceMemo.deleted) {
            throw DomainException(ErrorCode.MEMO_NOT_FOUND)
        }

        // 2. 文字起こしテキストが空でないことを確認
        if (input.editedTranscription.isBlank()) {
            throw DomainException(ErrorCode.INVALID_TRANSCRIPTION)
        }

        // 3. 既存フォルダーパスを取得（AI整形用）
        val existingFolderPaths = getExistingFolderPaths(input.userId)

        // 4. AI整形処理（再要約）
        var updatedMemo = voiceMemo.startFormatting()
        val formatResult = executionTimer.measure {
            runCatching {
                memoFormatter.format(
                    MemoFormatCommand(
                        userId = input.userId,
                        transcript = input.editedTranscription,
                        existingFolderPaths = existingFolderPaths,
                    ),
                )
            }.onFailure { ex ->
                logger.warn("AI memo formatting failed; fallback will be used", ex)
            }.getOrElse {
                fallbackFormatResult(input.editedTranscription)
            }
        }
        val memoFormat = formatResult.value

        // 5. フォルダーパスをIDに解決（必要に応じて作成）
        val folderId = resolveFolderId(input.userId, memoFormat.folderPath)

        // 6. 整形結果を適用
        val formattingFallbackUsed = memoFormat.title == "ボイスメモ" && memoFormat.tags.isEmpty()
        updatedMemo = updatedMemo.completeFormatting(
            title = memoFormat.title,
            content = memoFormat.content,
            tags = memoFormat.tags,
            fallbackUsed = formattingFallbackUsed,
            folderId = folderId,
        )

        // 7. 文字起こしテキストも更新
        if (!updatedMemo.transcription.isCompleted) {
            val errorCode = if (updatedMemo.transcription.isFailed)
                ErrorCode.TRANSCRIPTION_FAILED
            else
                ErrorCode.TRANSCRIPTION_NOT_COMPLETED
            throw DomainException(errorCode)
        }
        updatedMemo = updatedMemo.editTranscription(input.editedTranscription)

        // 8. 永続化
        val savedMemo = voiceMemoRepository.save(updatedMemo)

        return ResummarizeOutput(
            voiceMemo = savedMemo,
            formattingDuration = formatResult.duration,
        )
    }

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

    private fun fallbackFormatResult(transcriptionText: String): MemoFormatResult {
        return MemoFormatResult(
            title = "ボイスメモ",
            content = transcriptionText,
            tags = emptyList(),
            folderPath = null,
        )
    }
}

/**
 * 再要約入力
 */
data class ResummarizeInput(
    val memoId: UUID,
    val userId: UUID,
    val editedTranscription: String,
)

/**
 * 再要約出力
 */
data class ResummarizeOutput(
    val voiceMemo: VoiceMemo,
    val formattingDuration: Duration,
)
