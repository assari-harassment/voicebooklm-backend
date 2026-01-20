package com.assari.voicebooklm.presentation.controller.memo

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * メモ更新リクエスト
 *
 * すべてのフィールドはオプション。指定されたフィールドのみ更新される（PATCH）。
 */
data class UpdateMemoRequest(
    @field:Size(max = 100, message = "タイトルは100文字以内で入力してください")
    val title: String? = null,

    val content: String? = null,

    val tags: List<String>? = null,
)

/**
 * 再要約リクエスト
 */
data class ResummarizeRequest(
    @field:NotBlank(message = "編集された文字起こしテキストは必須です")
    val editedTranscription: String,
)
