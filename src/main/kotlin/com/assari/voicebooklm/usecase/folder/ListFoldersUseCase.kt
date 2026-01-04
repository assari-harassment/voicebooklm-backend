package com.assari.voicebooklm.usecase.folder

import com.assari.voicebooklm.domain.model.Folder
import com.assari.voicebooklm.domain.model.buildPath
import com.assari.voicebooklm.domain.repository.FolderRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * ユーザーのフォルダー一覧を取得するユースケース
 */
@Service
open class ListFoldersUseCase(
    private val folderRepository: FolderRepository,
) {
    @Transactional(readOnly = true)
    open suspend fun execute(input: ListFoldersInput): ListFoldersOutput {
        val folders = folderRepository.findByUserId(input.userId)
        val folderMap = folders.associateBy { it.id }

        val foldersWithPath = folders.map { folder ->
            val path = folder.buildPath(folderMap)
            FolderWithPath(
                folder = folder,
                path = path,
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
)
