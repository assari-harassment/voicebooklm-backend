package com.assari.voicebooklm.presentation.controller.memo

import com.assari.voicebooklm.usecase.memo.GetMemoOutput
import com.assari.voicebooklm.usecase.memo.GetTranscriptionOutput
import com.assari.voicebooklm.usecase.memo.ListMemosOutput
import com.assari.voicebooklm.usecase.memo.MemoWithFolder
import com.assari.voicebooklm.usecase.memo.ResummarizeOutput
import com.assari.voicebooklm.usecase.memo.UpdateMemoOutput
import java.time.Instant
import java.util.UUID

data class ListMemosResponse(
    val memos: List<MemoListItemResponse>,
    val total: Int,
    val hasMore: Boolean,
) {
    companion object {
        fun from(result: ListMemosOutput): ListMemosResponse {
            val memos = result.memos.map { memoWithFolder ->
                MemoListItemResponse.from(memoWithFolder)
            }
            return ListMemosResponse(
                memos = memos,
                total = result.total,
                hasMore = result.hasMore,
            )
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
    val folder: FolderInfo?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(memoWithFolder: MemoWithFolder): MemoListItemResponse {
            val memo = memoWithFolder.memo
            val folderInfo = memoWithFolder.folder?.let { folder ->
                FolderInfo(
                    id = folder.id,
                    name = folder.name,
                    path = memoWithFolder.folderPath ?: folder.name,
                )
            }
            return MemoListItemResponse(
                memoId = memo.id,
                title = memo.title,
                tags = memo.tags,
                transcriptionStatus = memo.transcription.status.name,
                formattingStatus = memo.formatting.status.name,
                folder = folderInfo,
                createdAt = memo.createdAt,
                updatedAt = memo.updatedAt,
            )
        }
    }
}

/**
 * フォルダー情報
 */
data class FolderInfo(
    val id: UUID,
    val name: String,
    val path: String,
)

/**
 * メモ詳細レスポンス
 */
data class MemoDetailResponse(
    val memoId: UUID,
    val title: String?,
    val content: String?,
    val tags: List<String>,
    val transcriptionText: String?,
    val transcriptionStatus: String,
    val formattingStatus: String,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(result: GetMemoOutput): MemoDetailResponse {
            val memo = result.memo
            return MemoDetailResponse(
                memoId = memo.id,
                title = memo.title,
                content = memo.content,
                tags = memo.tags,
                transcriptionText = memo.transcriptionText,
                transcriptionStatus = memo.transcription.status.name,
                formattingStatus = memo.formatting.status.name,
                createdAt = memo.createdAt,
                updatedAt = memo.updatedAt,
            )
        }

        fun from(result: UpdateMemoOutput): MemoDetailResponse {
            val memo = result.memo
            return MemoDetailResponse(
                memoId = memo.id,
                title = memo.title,
                content = memo.content,
                tags = memo.tags,
                transcriptionText = memo.transcriptionText,
                transcriptionStatus = memo.transcription.status.name,
                formattingStatus = memo.formatting.status.name,
                createdAt = memo.createdAt,
                updatedAt = memo.updatedAt,
            )
        }

        fun from(result: ResummarizeOutput): MemoDetailResponse {
            val memo = result.voiceMemo
            return MemoDetailResponse(
                memoId = memo.id,
                title = memo.title,
                content = memo.content,
                tags = memo.tags,
                transcriptionText = memo.transcriptionText,
                transcriptionStatus = memo.transcription.status.name,
                formattingStatus = memo.formatting.status.name,
                createdAt = memo.createdAt,
                updatedAt = memo.updatedAt,
            )
        }
    }
}

/**
 * 文字起こしテキストレスポンス
 */
data class TranscriptionResponse(
    val transcription: String,
) {
    companion object {
        fun from(result: GetTranscriptionOutput): TranscriptionResponse {
            return TranscriptionResponse(transcription = result.transcription)
        }
    }
}
