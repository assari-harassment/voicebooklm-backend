package com.assari.voicebooklm.usecase.folder

import com.assari.voicebooklm.domain.exception.DomainException
import com.assari.voicebooklm.domain.exception.ErrorCode
import com.assari.voicebooklm.domain.model.Folder
import com.assari.voicebooklm.domain.model.buildPath
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

        // 何も変更がない場合はパスを計算してそのまま返す
        if (input.newName == null && input.newParentId == null && !input.moveToRoot) {
            val allFolders = folderRepository.findByUserId(input.userId)
            val folderMap = allFolders.associateBy { it.id }
            val path = folder.buildPath(folderMap)
            return UpdateFolderOutput(folder = folder, path = path)
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
            val duplicateExists = folderRepository.existsByUserIdAndParentIdAndName(
                userId = input.userId,
                parentId = updatedFolder.parentId,
                name = updatedFolder.name,
                excludeId = folder.id,
            )
            if (duplicateExists) {
                throw DomainException(
                    ErrorCode.FOLDER_ALREADY_EXISTS,
                    "同じ名前のフォルダーが既に存在します: ${updatedFolder.name}"
                )
            }
        }

        val savedFolder = folderRepository.save(updatedFolder)

        // 5. パスを構築
        val allFoldersForPath = folderRepository.findByUserId(input.userId)
        val folderMapForPath = allFoldersForPath.associateBy { it.id }
        val path = savedFolder.buildPath(folderMapForPath)

        return UpdateFolderOutput(folder = savedFolder, path = path)
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
    val path: String,
)
