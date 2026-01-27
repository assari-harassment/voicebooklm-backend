package com.assari.voicebooklm.usecase.folder

import com.assari.voicebooklm.domain.model.Folder
import com.assari.voicebooklm.domain.model.buildPath
import com.assari.voicebooklm.domain.repository.FolderRepository
import com.assari.voicebooklm.domain.repository.VoiceMemoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * ユーザーのフォルダー一覧を取得するユースケース
 */
@Service
open class ListFoldersUseCase(
    private val folderRepository: FolderRepository,
    private val voiceMemoRepository: VoiceMemoRepository,
) {
    @Transactional(readOnly = true)
    open suspend fun execute(input: ListFoldersInput): ListFoldersOutput {
        val folders = folderRepository.findByUserId(input.userId)
        val folderMap = folders.associateBy { it.id }

        // 各フォルダーの子孫フォルダーIDを取得
        val folderIdsWithDescendants = folders.associate { folder ->
            val descendantIds = folderRepository.findDescendantIds(folder.id)
            folder.id to (listOf(folder.id) + descendantIds)
        }

        // メモ数を一括取得
        val memoCounts = voiceMemoRepository.countByFolderIds(input.userId, folderIdsWithDescendants)

        val foldersWithPath = folders.map { folder ->
            val path = folder.buildPath(folderMap)
            val memoCount = memoCounts[folder.id] ?: 0
            FolderWithPath(
                folder = folder,
                path = path,
                memoCount = memoCount,
            )
        }.sortedBy { it.path }

        return ListFoldersOutput(foldersWithPath)
    }
}

data class ListFoldersInput(
    val userId: UUID,
)

data class ListFoldersOutput(
    val folders: List<FolderWithPath>,
)

data class FolderWithPath(
    val folder: Folder,
    val path: String,
    val memoCount: Int,
)
