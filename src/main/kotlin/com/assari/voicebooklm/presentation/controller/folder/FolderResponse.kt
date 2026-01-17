package com.assari.voicebooklm.presentation.controller.folder

import com.assari.voicebooklm.domain.model.Folder
import com.assari.voicebooklm.usecase.folder.FolderWithPath
import com.assari.voicebooklm.usecase.folder.ListFoldersOutput
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
    val memoCount: Int,
) {
    companion object {
        fun from(folderWithPath: FolderWithPath): FolderResponse {
            return FolderResponse(
                id = folderWithPath.folder.id,
                name = folderWithPath.folder.name,
                parentId = folderWithPath.folder.parentId,
                path = folderWithPath.path,
                memoCount = folderWithPath.memoCount,
            )
        }

        fun from(folder: Folder, path: String): FolderResponse {
            return FolderResponse(
                id = folder.id,
                name = folder.name,
                parentId = folder.parentId,
                path = path,
                memoCount = 0, // フォルダー作成/更新APIではメモ数は返さない仕様のため常に0（一覧APIでは実装済み）
            )
        }
    }
}
