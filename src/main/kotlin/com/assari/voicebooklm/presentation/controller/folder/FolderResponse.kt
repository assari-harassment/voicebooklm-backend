package com.assari.voicebooklm.presentation.controller.folder

import com.assari.voicebooklm.domain.model.Folder
import com.assari.voicebooklm.usecase.folder.FolderWithPath
import com.assari.voicebooklm.usecase.folder.ListFoldersOutput
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

/**
 * フォルダー一覧レスポンス
 */
data class ListFoldersResponse(
    val folders: List<FolderResponse>,
) {
    companion object {
        fun from(output: ListFoldersOutput): ListFoldersResponse {
            val folders = output.folders.map { FolderResponse.from(it) }
            return ListFoldersResponse(folders = folders)
        }
    }
}

/**
 * フォルダー情報
 */
data class FolderResponse(
    val id: UUID,
    val name: String,
    val parentId: UUID?,
    val path: String,
) {
    companion object {
        fun from(folderWithPath: FolderWithPath): FolderResponse {
            return FolderResponse(
                id = folderWithPath.folder.id,
                name = folderWithPath.folder.name,
                parentId = folderWithPath.folder.parentId,
                path = folderWithPath.path,
            )
        }

        fun from(folder: Folder, path: String): FolderResponse {
            return FolderResponse(
                id = folder.id,
                name = folder.name,
                parentId = folder.parentId,
                path = path,
            )
        }
    }
}

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
