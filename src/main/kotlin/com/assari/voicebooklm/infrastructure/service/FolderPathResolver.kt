package com.assari.voicebooklm.infrastructure.service

import com.assari.voicebooklm.domain.model.Folder
import com.assari.voicebooklm.domain.model.buildPath
import com.assari.voicebooklm.domain.repository.FolderRepository
import com.github.f4b6a3.uuid.UuidCreator
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * AIが返したパス形式のフォルダーをIDに解決するサービス
 *
 * 既存フォルダーがあればそのIDを返し、なければ必要なフォルダーを作成する。
 */
@Service
class FolderPathResolver(
    private val folderRepository: FolderRepository,
) {
    /**
     * フォルダーパスをIDに解決する
     *
     * @param userId ユーザーID
     * @param folderPath スラッシュ区切りのフォルダーパス（例: "仕事/プロジェクトA"）
     * @return 解決されたフォルダーID
     */
    suspend fun resolveOrCreate(userId: UUID, folderPath: String): UUID {
        val segments = folderPath.split("/").map { it.trim() }.filter { it.isNotBlank() }
        if (segments.isEmpty()) {
            throw IllegalArgumentException("Folder path must not be empty")
        }

        // AI生成フォルダーは最大3階層まで
        val limitedSegments = segments.take(3)

        // ユーザーの全フォルダーを取得
        val allFolders = folderRepository.findByUserId(userId)
        val folderMapById = allFolders.associateBy { it.id }
        val folderMap = allFolders.associateBy { it.buildPath(folderMapById) }

        var currentParentId: UUID? = null
        var currentPath = ""

        for (segment in limitedSegments) {
            currentPath = if (currentPath.isEmpty()) segment else "$currentPath/$segment"

            val existingFolder = folderMap[currentPath]
            if (existingFolder != null) {
                currentParentId = existingFolder.id
            } else {
                // フォルダーを新規作成
                val newFolder = Folder.create(
                    id = UuidCreator.getTimeOrderedEpoch(),
                    userId = userId,
                    name = segment,
                    parentId = currentParentId,
                )
                val savedFolder = folderRepository.save(newFolder)
                currentParentId = savedFolder.id
            }
        }

        return currentParentId!!
    }
}
