package com.assari.voicebooklm.presentation.controller.memo

import jakarta.validation.constraints.Size
import java.util.UUID

/**
 * メモ更新リクエスト
 *
 * すべてのフィールドはオプション。指定されたフィールドのみ更新される（PATCH）。
 */
data class UpdateMemoRequest(
    @field:Size(max = 100, message = "タイトルは100文字以内で入力してください")
    val title: String? = null,

    val content: String? = null,

    val tagIds: List<UUID>? = null,
)
