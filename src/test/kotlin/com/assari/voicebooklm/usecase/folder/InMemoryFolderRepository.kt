package com.assari.voicebooklm.usecase.folder

import com.assari.voicebooklm.domain.model.Folder
import com.assari.voicebooklm.domain.model.buildPath
import com.assari.voicebooklm.domain.repository.FolderRepository
import java.util.UUID

/**
 * インメモリで動作する FolderRepository のテストダブル。
 *
 * フォルダー関連のユースケーステストで使用する。
 */
class InMemoryFolderRepository(
    initialFolders: List<Folder> = emptyList(),
) : FolderRepository {
    private val folders = initialFolders.toMutableList()

    override suspend fun save(folder: Folder): Folder {
        folders.removeIf { it.id == folder.id }
        folders += folder
        return folder
    }

    override suspend fun findById(id: UUID): Folder? = folders.find { it.id == id }

    override suspend fun findByUserId(userId: UUID): List<Folder> = folders.filter { it.userId == userId }

    override suspend fun findByUserIdAndParentId(userId: UUID, parentId: UUID?): List<Folder> =
        folders.filter { it.userId == userId && it.parentId == parentId }

    override suspend fun findByUserIdAndPath(userId: UUID, path: String): Folder? {
        val userFolders = folders.filter { it.userId == userId }
        val folderMap = userFolders.associateBy { it.id }
        return userFolders.find { folder -> folder.buildPath(folderMap) == path }
    }

    override suspend fun findDescendantIds(folderId: UUID): List<UUID> {
        val result = mutableListOf<UUID>()
        val queue = ArrayDeque<UUID>()
        queue.add(folderId)

        while (queue.isNotEmpty()) {
            val currentId = queue.removeFirst()
            val children = folders.filter { it.parentId == currentId }
            for (child in children) {
                result.add(child.id)
                queue.add(child.id)
            }
        }

        return result
    }

    override suspend fun delete(id: UUID) {
        folders.removeIf { it.id == id }
    }

    override suspend fun existsByUserIdAndParentIdAndName(userId: UUID, parentId: UUID?, name: String): Boolean =
        folders.any { it.userId == userId && it.parentId == parentId && it.name == name }

    override fun deleteByUserId(userId: UUID) {
        folders.removeIf { it.userId == userId }
    }
}
