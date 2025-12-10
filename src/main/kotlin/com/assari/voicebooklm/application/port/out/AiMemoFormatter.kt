package com.assari.voicebooklm.application.port.out

import java.util.UUID

/**
 * 文字起こしテキストをメモとして整形する AI ポート。
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
