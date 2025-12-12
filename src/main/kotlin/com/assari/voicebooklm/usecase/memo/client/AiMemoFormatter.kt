package com.assari.voicebooklm.usecase.memo.client

import java.util.UUID

/**
 * 文字起こしテキストをメモとして整形する AI クライアント。
 */
interface AiMemoFormatter {
    suspend fun format(command: AiMemoFormatCommand): AiMemoDraft
}

/**
 * 整形要求。
 */
data class AiMemoFormatCommand(
    val userId: UUID,
    val transcript: String,
)

/**
 * AI が生成したメモ下書き。
 */
data class AiMemoDraft(
    val title: String,
    val content: String,
    val tags: List<String>,
)
