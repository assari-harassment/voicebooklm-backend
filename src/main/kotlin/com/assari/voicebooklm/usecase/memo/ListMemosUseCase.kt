package com.assari.voicebooklm.usecase.memo

import com.assari.voicebooklm.domain.model.Folder
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
        // 1. メモ一覧を取得
        var memos = voiceMemoRepository.findByUserId(input.userId)

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
        val memosWithFolder = memos.map { memo ->
            val folder = memo.formatting.folderId?.let { folderMap[it] }
            val path = memo.formatting.folderId?.let { folderPathMap[it] }
            MemoWithFolder(memo = memo, folder = folder, folderPath = path)
        }

        return ListMemosOutput(memosWithFolder)
    }
}

/**
 * メモ一覧取得 Input
 */
data class ListMemosInput(
    val userId: UUID,
    /** フォルダーIDでフィルタリング（null の場合は全件） */
    val folderId: UUID? = null,
    /** true の場合、子孫フォルダーのメモも含める */
    val includeDescendants: Boolean = false,
    /** true の場合、未分類メモのみ取得 */
    val uncategorizedOnly: Boolean = false,
)

/**
 * メモ一覧取得 Output
 */
data class ListMemosOutput(
    val memos: List<MemoWithFolder>,
)

/**
 * フォルダー情報付きメモ
 */
data class MemoWithFolder(
    val memo: VoiceMemo,
    val folder: Folder?,
    val folderPath: String?,
)
