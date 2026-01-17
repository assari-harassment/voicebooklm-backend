package com.assari.voicebooklm.usecase.memo

import com.assari.voicebooklm.domain.model.Folder
import com.assari.voicebooklm.domain.model.MemoSortField
import com.assari.voicebooklm.domain.model.SortOrder
import com.assari.voicebooklm.domain.model.VoiceMemo
import com.assari.voicebooklm.domain.model.buildPath
import com.assari.voicebooklm.domain.repository.FolderRepository
import com.assari.voicebooklm.domain.repository.VoiceMemoRepository
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * ユーザーのメモ一覧を取得するユースケース
 */
@Service
open class ListMemosUseCase(
    private val voiceMemoRepository: VoiceMemoRepository,
    private val folderRepository: FolderRepository,
) {
    // 読み取り専用の一覧取得処理
    @Transactional(readOnly = true)
    open suspend fun execute(input: ListMemosInput): ListMemosOutput {
        // 1. メモ一覧を取得（キーワード検索が指定されている場合はキーワードで絞り込む）
        var memos = if (!input.keyword.isNullOrBlank()) {
            voiceMemoRepository.findByUserIdWithKeyword(input.userId, input.keyword)
        } else {
            voiceMemoRepository.findByUserId(input.userId)
        }

        // 2. フォルダーフィルタリング
        memos = when {
            input.uncategorizedOnly -> memos.filter { it.formatting.folderId == null }
            input.folderId != null -> {
                if (input.includeDescendants) {
                    // 子孫フォルダーのメモも含める
                    val descendantIds = folderRepository.findDescendantIds(input.folderId)
                    val targetFolderIds = listOf(input.folderId) + descendantIds
                    memos.filter { it.formatting.folderId in targetFolderIds }
                } else {
                    // 指定フォルダーのメモのみ
                    memos.filter { it.formatting.folderId == input.folderId }
                }
            }
            else -> memos
        }

        // 3. フォルダー情報を取得
        val folders = folderRepository.findByUserId(input.userId)
        val folderMap = folders.associateBy { it.id }
        val folderPathMap = folders.associate { it.id to it.buildPath(folderMap) }

        // 4. メモにフォルダー情報を付与
        var memosWithFolder = memos.map { memo ->
            val folder = memo.formatting.folderId?.let { folderMap[it] }
            val path = memo.formatting.folderId?.let { folderPathMap[it] }
            MemoWithFolder(memo = memo, folder = folder, folderPath = path)
        }

        // 5. ソート処理
        memosWithFolder = when (input.sortBy) {
            MemoSortField.UPDATED_AT -> {
                if (input.sortOrder == SortOrder.ASC) {
                    memosWithFolder.sortedBy { it.memo.updatedAt }
                } else {
                    memosWithFolder.sortedByDescending { it.memo.updatedAt }
                }
            }
            MemoSortField.CREATED_AT -> {
                if (input.sortOrder == SortOrder.ASC) {
                    memosWithFolder.sortedBy { it.memo.createdAt }
                } else {
                    memosWithFolder.sortedByDescending { it.memo.createdAt }
                }
            }
            MemoSortField.TITLE -> {
                // タイトルがnullのメモは、昇順の場合は先頭に、降順の場合は末尾に配置される
                if (input.sortOrder == SortOrder.ASC) {
                    memosWithFolder.sortedBy { it.memo.formatting.title }
                } else {
                    memosWithFolder.sortedByDescending { it.memo.formatting.title }
                }
            }
        }

        // 6. 全件数を保存（ページネーション前）
        val total = memosWithFolder.size

        // 7. offset適用
        val offset = input.offset ?: 0
        memosWithFolder = memosWithFolder.drop(offset)

        // 8. limit適用
        if (input.limit != null && input.limit > 0) {
            memosWithFolder = memosWithFolder.take(input.limit)
        }

        // 9. 次のページがあるかを計算
        val hasMore = offset + memosWithFolder.size < total

        return ListMemosOutput(memosWithFolder, total, hasMore)
    }
}

data class ListMemosInput(
    val userId: UUID,
    /** フォルダーIDでフィルタリング（null の場合は全件） */
    val folderId: UUID? = null,
    /** true の場合、子孫フォルダーのメモも含める */
    val includeDescendants: Boolean = false,
    /** true の場合、未分類メモのみ取得 */
    val uncategorizedOnly: Boolean = false,
    /** キーワード検索（null または空の場合は全件） */
    val keyword: String? = null,
    /** ソート項目（デフォルト: updated_at） */
    val sortBy: MemoSortField = MemoSortField.UPDATED_AT,
    /** ソート順序（デフォルト: desc） */
    val sortOrder: SortOrder = SortOrder.DESC,
    /** 取得件数制限（null の場合は全件） */
    val limit: Int? = null,
    /** 取得開始位置（0から開始、null の場合は0） */
    val offset: Int? = null,
)

data class ListMemosOutput(
    val memos: List<MemoWithFolder>,
    /** 全件数（フィルタリング・検索後、ページネーション前の件数） */
    val total: Int,
    /** 次のページが存在するか */
    val hasMore: Boolean,
)

/**
 * フォルダー情報付きメモ
 */
data class MemoWithFolder(
    val memo: VoiceMemo,
    val folder: Folder?,
    val folderPath: String?,
)
