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
)

/**
 * 整形結果
 */
data class MemoFormatResult(
    val title: String,
    val content: String,
    val tags: List<String>,
)
