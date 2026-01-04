package com.assari.voicebooklm.usecase.folder

import com.assari.voicebooklm.domain.exception.DomainException
import com.assari.voicebooklm.domain.exception.ErrorCode
import com.assari.voicebooklm.domain.model.Folder
import com.assari.voicebooklm.domain.repository.FolderRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * フォルダーを更新するユースケース
 *
 * リネームと移動を統合して処理する。
 */
@Service
open class UpdateFolderUseCase(
    private val folderRepository: FolderRepository,
) {
    @Transactional
    open suspend fun execute(input: UpdateFolderInput): UpdateFolderOutput {
        // 1. フォルダーの存在確認
        val folder = folderRepository.findById(input.folderId)
            ?: throw DomainException(ErrorCode.FOLDER_NOT_FOUND, "フォルダーが見つかりません: ${input.folderId}")

        // ユーザーIDの確認
        if (folder.userId != input.userId) {
            throw DomainException(ErrorCode.FOLDER_NOT_FOUND, "フォルダーが見つかりません: ${input.folderId}")
        }

        // 何も変更がない場合はそのまま返す
        if (input.newName == null && input.newParentId == null && !input.moveToRoot) {
            return UpdateFolderOutput(folder = folder)
        }

        var updatedFolder = folder

        // 2. 移動処理（newParentId指定時または moveToRoot 時）
        val targetParentId = when {
            input.moveToRoot -> null
            input.newParentId != null -> input.newParentId
            else -> folder.parentId
        }

        if (targetParentId != folder.parentId || input.moveToRoot) {
            if (targetParentId != null) {
                // 移動先フォルダーの存在確認
                val targetFolder = folderRepository.findById(targetParentId)
                    ?: throw DomainException(
                        ErrorCode.FOLDER_NOT_FOUND,
                        "移動先フォルダーが見つかりません: $targetParentId"
                    )

                // 移動先が別ユーザーのものでないかチェック
                if (targetFolder.userId != input.userId) {
                    throw DomainException(
                        ErrorCode.FOLDER_NOT_FOUND,
                        "移動先フォルダーが見つかりません: $targetParentId"
                    )
                }

                // 循環参照チェック（移動先が自分の子孫でないこと）
                val descendantIds = folderRepository.findDescendantIds(input.folderId)
                if (targetParentId in descendantIds) {
                    throw DomainException(
                        ErrorCode.FOLDER_CIRCULAR_REFERENCE,
                        "移動先が自分の子孫フォルダーのため移動できません"
                    )
                }
            }

            updatedFolder = updatedFolder.moveTo(targetParentId)
        }

        // 3. リネーム処理
        val targetName = input.newName?.trim() ?: folder.name
        if (targetName != folder.name) {
            updatedFolder = updatedFolder.rename(targetName)
        }

        // 4. 同名フォルダーの重複チェック（移動先またはリネーム後）
        if (targetName != folder.name || targetParentId != folder.parentId) {
            val exists = folderRepository.existsByUserIdAndParentIdAndName(
                userId = input.userId,
                parentId = updatedFolder.parentId,
                name = updatedFolder.name,
            )
            // 自分自身は除外（リネームなしで移動のみの場合）
            if (exists && (updatedFolder.name != folder.name || updatedFolder.parentId != folder.parentId)) {
                // 既存の同名フォルダーが自分自身でないかチェック
                val existingFolder = folderRepository.findByUserIdAndPath(
                    userId = input.userId,
                    path = buildPathForCheck(input.userId, updatedFolder.parentId, updatedFolder.name),
                )
                if (existingFolder != null && existingFolder.id != folder.id) {
                    throw DomainException(
                        ErrorCode.FOLDER_ALREADY_EXISTS,
                        "同じ名前のフォルダーが既に存在します: ${updatedFolder.name}"
                    )
                }
            }
        }

        val savedFolder = folderRepository.save(updatedFolder)
        return UpdateFolderOutput(folder = savedFolder)
    }

    private suspend fun buildPathForCheck(userId: UUID, parentId: UUID?, name: String): String {
        if (parentId == null) {
            return name
        }
        val allFolders = folderRepository.findByUserId(userId)
        val folderMap = allFolders.associateBy { it.id }

        val pathSegments = mutableListOf(name)
        var currentParentId: UUID? = parentId
        while (currentParentId != null) {
            val parent = folderMap[currentParentId] ?: break
            pathSegments.add(0, parent.name)
            currentParentId = parent.parentId
        }
        return pathSegments.joinToString("/")
    }
}

data class UpdateFolderInput(
    val userId: UUID,
    val folderId: UUID,
    val newName: String? = null,
    val newParentId: UUID? = null,
    val moveToRoot: Boolean = false,
)

data class UpdateFolderOutput(
    val folder: Folder,
)
