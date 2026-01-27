package com.assari.voicebooklm.usecase.folder

import com.assari.voicebooklm.domain.exception.DomainException
import com.assari.voicebooklm.domain.exception.ErrorCode
import com.assari.voicebooklm.domain.repository.FolderRepository
import com.assari.voicebooklm.domain.repository.VoiceMemoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * フォルダー削除ユースケース
 *
 * 子フォルダーまたはメモが存在する場合は削除できない。
 */
@Service
open class DeleteFolderUseCase(
    private val folderRepository: FolderRepository,
    private val voiceMemoRepository: VoiceMemoRepository,
) {
    @Transactional
    open suspend fun execute(input: DeleteFolderInput) {
        // 1. フォルダーの存在確認
        val folder = folderRepository.findById(input.folderId)
            ?: throw DomainException(ErrorCode.FOLDER_NOT_FOUND, "フォルダーが見つかりません: ${input.folderId}")

        // 2. ユーザー権限チェック
        if (folder.userId != input.userId) {
            throw DomainException(ErrorCode.FOLDER_NOT_FOUND, "フォルダーが見つかりません: ${input.folderId}")
        }

        // 3. 子フォルダーの存在チェック
        val descendantIds = folderRepository.findDescendantIds(input.folderId)
        if (descendantIds.isNotEmpty()) {
            throw DomainException(
                ErrorCode.FOLDER_HAS_CHILDREN,
                "子フォルダーが存在するため削除できません"
            )
        }

        // 4. メモの存在チェック
        val hasMemos = voiceMemoRepository.existsByUserIdAndFolderId(input.userId, input.folderId)
        if (hasMemos) {
            throw DomainException(
                ErrorCode.FOLDER_HAS_MEMOS,
                "フォルダー内にメモが存在するため削除できません"
            )
        }

        // 5. フォルダー削除
        folderRepository.delete(input.folderId)
    }
}

/**
 * フォルダー削除Input
 */
data class DeleteFolderInput(
    val userId: UUID,
    val folderId: UUID,
)

