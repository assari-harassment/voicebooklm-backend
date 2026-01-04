package com.assari.voicebooklm.usecase.folder

import com.assari.voicebooklm.domain.exception.DomainException
import com.assari.voicebooklm.domain.exception.ErrorCode
import com.assari.voicebooklm.domain.model.Folder
import com.assari.voicebooklm.domain.repository.FolderRepository
import com.github.f4b6a3.uuid.UuidCreator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * フォルダーを作成するユースケース
 */
@Service
open class CreateFolderUseCase(
    private val folderRepository: FolderRepository,
) {
    @Transactional
    open suspend fun execute(input: CreateFolderInput): CreateFolderOutput {
        // 1. 親フォルダーの存在確認（parentId指定時）
        if (input.parentId != null) {
            val parentFolder = folderRepository.findById(input.parentId)
            if (parentFolder == null) {
                throw DomainException(
                    ErrorCode.FOLDER_NOT_FOUND,
                    "親フォルダーが見つかりません: ${input.parentId}"
                )
            }
            // 親フォルダーが別ユーザーのものでないかチェック
            if (parentFolder.userId != input.userId) {
                throw DomainException(
                    ErrorCode.FOLDER_NOT_FOUND,
                    "親フォルダーが見つかりません: ${input.parentId}"
                )
            }
        }

        // 2. 同名フォルダーの重複チェック
        val exists = folderRepository.existsByUserIdAndParentIdAndName(
            userId = input.userId,
            parentId = input.parentId,
            name = input.name.trim(),
        )
        if (exists) {
            throw DomainException(
                ErrorCode.FOLDER_ALREADY_EXISTS,
                "同じ名前のフォルダーが既に存在します: ${input.name}"
            )
        }

        // 3. フォルダー作成
        val folder = Folder.create(
            id = UuidCreator.getTimeOrderedEpoch(),
            userId = input.userId,
            name = input.name,
            parentId = input.parentId,
        )

        val savedFolder = folderRepository.save(folder)
        return CreateFolderOutput(folder = savedFolder)
    }
}

data class CreateFolderInput(
    val userId: UUID,
    val name: String,
    val parentId: UUID? = null,
)

data class CreateFolderOutput(
    val folder: Folder,
)
