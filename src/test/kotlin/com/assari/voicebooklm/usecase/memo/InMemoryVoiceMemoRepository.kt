package com.assari.voicebooklm.usecase.memo

import com.assari.voicebooklm.domain.model.VoiceMemo
import com.assari.voicebooklm.domain.repository.VoiceMemoRepository
import java.util.UUID

/**
 * インメモリで動作する VoiceMemoRepository のテストダブル。
 */
internal class InMemoryVoiceMemoRepository(
    initialMemos: List<VoiceMemo> = emptyList(),
) : VoiceMemoRepository {
    private val memos = initialMemos.toMutableList()

    override suspend fun save(voiceMemo: VoiceMemo): VoiceMemo {
        // 既存のメモを更新、なければ追加
        val index = memos.indexOfFirst { it.id == voiceMemo.id }
        if (index >= 0) {
            memos[index] = voiceMemo
        } else {
            memos += voiceMemo
        }
        return voiceMemo
    }

    override suspend fun findById(id: UUID): VoiceMemo? =
        memos.find { it.id == id && !it.deleted }

    override suspend fun findByUserId(userId: UUID): List<VoiceMemo> =
        memos.filter { it.userId == userId && !it.deleted }

    override suspend fun findByUserIdWithKeyword(userId: UUID, keyword: String): List<VoiceMemo> =
        memos.filter { memo ->
            memo.userId == userId && !memo.deleted &&
                (memo.formatting.title?.contains(keyword, ignoreCase = true) == true ||
                    memo.formatting.content?.contains(keyword, ignoreCase = true) == true)
        }

    override fun deleteByUserId(userId: UUID) {
        memos.removeIf { it.userId == userId }
    }

    override suspend fun existsByUserIdAndFolderId(userId: UUID, folderId: UUID): Boolean =
        memos.any { it.userId == userId && it.formatting.folderId == folderId && !it.deleted }

    override suspend fun countByFolderIds(
        userId: UUID,
        folderIdsWithDescendants: Map<UUID, List<UUID>>,
    ): Map<UUID, Int> {
        // ユーザーの削除されていないメモをフィルタ
        val userMemos = memos.filter { it.userId == userId && !it.deleted }

        // 各フォルダーIDについて、そのフォルダーと子孫フォルダーに属するメモをカウント
        return folderIdsWithDescendants.mapValues { (_, descendantIds) ->
            userMemos.count { memo ->
                memo.formatting.folderId != null && memo.formatting.folderId in descendantIds
            }
        }
    }

    /**
     * テスト用：削除済みメモも含めて取得するメソッド
     */
    fun findByIdIncludingDeleted(id: UUID): VoiceMemo? =
        memos.find { it.id == id }
}