package com.assari.voicebooklm.presentation.controller.folder

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

/**
 * フォルダー作成リクエスト
 */
data class CreateFolderRequest(
    @field:NotBlank(message = "フォルダー名は必須です")
    @field:Size(max = 50, message = "フォルダー名は50文字以内で入力してください")
    val name: String,
    val parentId: UUID? = null,
)

/**
 * フォルダー更新リクエスト
 *
 * name と parentId はオプション。両方指定可能。
 * parentId を null に設定する場合は moveToRoot を true にする。
 */
data class UpdateFolderRequest(
    @field:Size(max = 50, message = "フォルダー名は50文字以内で入力してください")
    val name: String? = null,
    val parentId: UUID? = null,
    val moveToRoot: Boolean = false,
)
