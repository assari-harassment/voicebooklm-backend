package com.assari.voicebooklm.presentation.controller.memo

import com.assari.voicebooklm.usecase.memo.ListMemosOutput
import java.time.Instant
import java.util.UUID

/**
 * メモ一覧レスポンス
 */
data class ListMemosResponse(
    val memos: List<MemoListItemResponse>,
) {
    companion object {
        fun from(result: ListMemosOutput): ListMemosResponse {
            // 検索未対応のため、一覧取得結果をそのまま詰める
            val memos = result.memos.map { memo ->
                MemoListItemResponse(
                    memoId = memo.id,
                    // 整形未完了のメモは null を返し、未整形と空文字を区別する
                    title = memo.title,
                    tags = memo.tags,
                    transcriptionStatus = memo.transcription.status.name,
                    formattingStatus = memo.formatting.status.name,
                    createdAt = memo.createdAt,
                    updatedAt = memo.updatedAt,
                )
            }
            return ListMemosResponse(memos = memos)
        }
    }
}

/**
 * メモ一覧の要素
 */
data class MemoListItemResponse(
    val memoId: UUID,
    val title: String?,
    val tags: List<String>,
    val transcriptionStatus: String,
    val formattingStatus: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)
