package com.assari.voicebooklm.domain.gateway

import java.util.UUID

/**
 * 文字起こしテキストをメモとして整形するゲートウェイ
 */
interface MemoFormatter {
    suspend fun format(command: MemoFormatCommand): MemoFormatResult
}

/**
 * 整形要求
 */
data class MemoFormatCommand(
    val userId: UUID,
    val transcript: String,
    /** 既存フォルダーのパス一覧（AIが参考にする） */
    val existingFolderPaths: List<String> = emptyList(),
    /** ユーザーがフォルダを指定済みか（folderId または folderPath）。true のときAIにフォルダ一覧を渡さない */
    val folderSpecifiedByUser: Boolean = false,
    /** ユーザーが指定したタグ。プロンプトに「必ず含める」として渡す */
    val userSpecifiedTags: List<String> = emptyList(),
)

/**
 * 整形結果
 */
data class MemoFormatResult(
    val title: String,
    val content: String,
    val tags: List<String>,
    /** AIが提案したフォルダーパス（分類できなかった場合は null） */
    val folderPath: String? = null,
)
