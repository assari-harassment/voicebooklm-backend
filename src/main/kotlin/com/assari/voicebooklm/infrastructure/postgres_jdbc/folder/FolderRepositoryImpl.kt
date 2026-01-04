package com.assari.voicebooklm.infrastructure.postgres_jdbc.folder

import com.assari.voicebooklm.domain.model.Folder
import com.assari.voicebooklm.domain.repository.FolderRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * フォルダーリポジトリ実装
 *
 * Domain Layer の FolderRepository インターフェースを実装する。
 */
@Repository
class FolderRepositoryImpl(
    private val folderJdbcRepository: FolderJdbcRepository
) : FolderRepository {

    override suspend fun save(folder: Folder): Folder {
        val entity = FolderEntity.fromDomain(folder)
        val savedEntity = folderJdbcRepository.save(entity)
        return savedEntity.toDomain()
    }

    override suspend fun findById(id: UUID): Folder? {
        return folderJdbcRepository.findByIdOrNull(id)?.toDomain()
    }

    override suspend fun findByUserId(userId: UUID): List<Folder> {
        return folderJdbcRepository.findByUserId(userId).map { it.toDomain() }
    }

    override suspend fun findByUserIdAndParentId(userId: UUID, parentId: UUID?): List<Folder> {
        return if (parentId == null) {
            folderJdbcRepository.findByUserIdAndParentIdIsNull(userId)
        } else {
            folderJdbcRepository.findByUserIdAndParentId(userId, parentId)
        }.map { it.toDomain() }
    }

    override suspend fun findByUserIdAndPath(userId: UUID, path: String): Folder? {
        if (path.isBlank()) return null

        val pathSegments = path.split("/").filter { it.isNotBlank() }
        if (pathSegments.isEmpty()) return null

        var currentFolder: Folder? = null
        for (segment in pathSegments) {
            val parentId = currentFolder?.id
            currentFolder = if (parentId == null) {
                folderJdbcRepository.findByUserIdAndParentIdIsNullAndName(userId, segment)?.toDomain()
            } else {
                folderJdbcRepository.findByUserIdAndParentIdAndName(userId, parentId, segment)?.toDomain()
            }
            if (currentFolder == null) return null
        }
        return currentFolder
    }

    override suspend fun findDescendantIds(folderId: UUID): List<UUID> {
        val descendants = mutableListOf<UUID>()
        val folder = folderJdbcRepository.findByIdOrNull(folderId)?.toDomain() ?: return emptyList()

        fun collectDescendants(parentId: UUID) {
            val children = folderJdbcRepository.findByUserIdAndParentId(folder.userId, parentId)
            for (child in children) {
                descendants.add(child.id)
                collectDescendants(child.id)
            }
        }

        collectDescendants(folderId)
        return descendants
    }

    override suspend fun delete(id: UUID) {
        folderJdbcRepository.deleteById(id)
    }

    override suspend fun existsByUserIdAndParentIdAndName(
        userId: UUID,
        parentId: UUID?,
        name: String,
        excludeId: UUID?,
    ): Boolean {
        return when {
            parentId == null && excludeId == null ->
                folderJdbcRepository.existsByUserIdAndParentIdIsNullAndName(userId, name)
            parentId == null && excludeId != null ->
                folderJdbcRepository.existsByUserIdAndParentIdIsNullAndNameExcludingId(userId, name, excludeId)
            parentId != null && excludeId == null ->
                folderJdbcRepository.existsByUserIdAndParentIdAndName(userId, parentId, name)
            else ->
                folderJdbcRepository.existsByUserIdAndParentIdAndNameExcludingId(userId, parentId!!, name, excludeId!!)
        }
    }

    override fun deleteByUserId(userId: UUID) {
        folderJdbcRepository.deleteByUserId(userId)
    }
}
