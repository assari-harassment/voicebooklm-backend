package com.assari.voicebooklm.usecase.memo

import com.assari.voicebooklm.domain.exception.DomainException
import com.assari.voicebooklm.domain.exception.ErrorCode
import com.assari.voicebooklm.domain.model.VoiceMemo
import com.assari.voicebooklm.domain.repository.FolderRepository
import com.assari.voicebooklm.domain.repository.VoiceMemoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * メモ更新ユースケース
 *
 * 指定されたメモの部分更新を行う（PATCH）。
 * 整形完了済みのメモのみ更新可能。
 * タイトル、本文、タグ、フォルダーを個別または組み合わせて更新できる。
 */
@Service
open class UpdateMemoUseCase(
    private val voiceMemoRepository: VoiceMemoRepository,
    private val folderRepository: FolderRepository,
) {
    /**
     * メモを更新する
     *
     * @param input メモ更新Input
     * @return 更新後のメモ
     * @throws DomainException メモが見つからない、権限がない、整形未完了、またはフォルダーが存在しない場合
     */
    @Transactional
    open suspend fun execute(input: UpdateMemoInput): UpdateMemoOutput {
        // 1. メモを取得
        val memo = voiceMemoRepository.findById(input.memoId)
            ?: throw DomainException(ErrorCode.MEMO_NOT_FOUND)

        // 2. 権限チェック（自分のメモかどうか）
        // メモの存在を推測されないよう、403ではなく404を返す
        if (memo.userId != input.userId) {
            throw DomainException(ErrorCode.MEMO_NOT_FOUND)
        }

        // 3. 整形完了チェック
        if (!memo.formatting.isCompleted) {
            throw DomainException(ErrorCode.MEMO_NOT_COMPLETED)
        }

        // 4. ドメインメソッドで更新
        var updated = memo

        // タイトル更新
        if (input.title != null) {
            updated = updated.changeTitle(input.title)
        }

        // 本文更新
        if (input.content != null) {
            updated = updated.changeContent(input.content)
        }

        // タグ更新
        if (input.tags != null) {
            updated = updated.changeTags(input.tags)
        }

        // フォルダー更新
        if (input.removeFolder) {
            // フォルダー解除
            updated = updated.copy(
                formatting = updated.formatting.copy(folderId = null),
                updatedAt = java.time.Instant.now(),
            )
        } else if (input.folderId != null) {
            // フォルダー設定
            // フォルダーの存在と所有者確認
            val folder = folderRepository.findById(input.folderId)
                ?: throw DomainException(ErrorCode.FOLDER_NOT_FOUND)

            if (folder.userId != input.userId) {
                throw DomainException(ErrorCode.FOLDER_NOT_FOUND)
            }

            updated = updated.copy(
                formatting = updated.formatting.copy(folderId = input.folderId),
                updatedAt = java.time.Instant.now(),
            )
        }

        // 5. 永続化
        val savedMemo = voiceMemoRepository.save(updated)

        // 6. 結果を返却
        return UpdateMemoOutput(savedMemo)
    }
}

/**
 * メモ更新Input
 */
data class UpdateMemoInput(
    val memoId: UUID,
    val userId: UUID,
    val title: String?,
    val content: String?,
    val tags: List<String>?,
    val folderId: UUID?,
    val removeFolder: Boolean,
)

/**
 * メモ更新Output
 */
data class UpdateMemoOutput(
    val memo: VoiceMemo,
)