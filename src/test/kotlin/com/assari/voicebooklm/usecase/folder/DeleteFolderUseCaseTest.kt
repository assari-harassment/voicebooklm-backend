package com.assari.voicebooklm.usecase.folder

import com.assari.voicebooklm.domain.exception.DomainException
import com.assari.voicebooklm.domain.exception.ErrorCode
import com.assari.voicebooklm.domain.model.Folder
import com.assari.voicebooklm.domain.model.Formatting
import com.assari.voicebooklm.domain.model.FormattingStatus
import com.assari.voicebooklm.domain.model.Transcription
import com.assari.voicebooklm.domain.model.TranscriptionStatus
import com.assari.voicebooklm.domain.model.VoiceMemo
import com.assari.voicebooklm.usecase.memo.InMemoryVoiceMemoRepository
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * DeleteFolderUseCase の振る舞いをテストダブルで検証。
 */
class DeleteFolderUseCaseTest {

    @Test
    fun `空のフォルダーを削除できる`() = runTest {
        val userId = UUID.randomUUID()
        val folder = Folder.create(
            id = UUID.randomUUID(),
            userId = userId,
            name = "仕事",
            parentId = null,
        )
        val folderRepository = InMemoryFolderRepository(initialFolders = listOf(folder))
        val voiceMemoRepository = InMemoryVoiceMemoRepository()
        val useCase = DeleteFolderUseCase(folderRepository, voiceMemoRepository)

        useCase.execute(DeleteFolderInput(userId = userId, folderId = folder.id))

        // フォルダーが削除されていることを確認
        assertNull(folderRepository.findById(folder.id))
    }

    @Test
    fun `存在しないフォルダーを削除しようとするとFOLDER_NOT_FOUNDエラーになる`() = runTest {
        val userId = UUID.randomUUID()
        val nonExistentFolderId = UUID.randomUUID()

        val folderRepository = InMemoryFolderRepository()
        val voiceMemoRepository = InMemoryVoiceMemoRepository()
        val useCase = DeleteFolderUseCase(folderRepository, voiceMemoRepository)

        val exception = assertThrows<DomainException> {
            useCase.execute(DeleteFolderInput(userId = userId, folderId = nonExistentFolderId))
        }

        assertEquals(ErrorCode.FOLDER_NOT_FOUND, exception.code)
    }

    @Test
    fun `他人のフォルダーを削除しようとするとFOLDER_NOT_FOUNDエラーになる`() = runTest {
        val ownerId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        val folder = Folder.create(
            id = UUID.randomUUID(),
            userId = ownerId,
            name = "仕事",
            parentId = null,
        )

        val folderRepository = InMemoryFolderRepository(initialFolders = listOf(folder))
        val voiceMemoRepository = InMemoryVoiceMemoRepository()
        val useCase = DeleteFolderUseCase(folderRepository, voiceMemoRepository)

        val exception = assertThrows<DomainException> {
            useCase.execute(DeleteFolderInput(userId = otherUserId, folderId = folder.id))
        }

        assertEquals(ErrorCode.FOLDER_NOT_FOUND, exception.code)

        // フォルダーは削除されていないことを確認
        assertEquals(folder, folderRepository.findById(folder.id))
    }

    @Test
    fun `子フォルダーが存在する場合は削除できない`() = runTest {
        val userId = UUID.randomUUID()
        val parentFolder = Folder.create(
            id = UUID.randomUUID(),
            userId = userId,
            name = "仕事",
            parentId = null,
        )
        val childFolder = Folder.create(
            id = UUID.randomUUID(),
            userId = userId,
            name = "プロジェクトA",
            parentId = parentFolder.id,
        )

        val folderRepository = InMemoryFolderRepository(initialFolders = listOf(parentFolder, childFolder))
        val voiceMemoRepository = InMemoryVoiceMemoRepository()
        val useCase = DeleteFolderUseCase(folderRepository, voiceMemoRepository)

        val exception = assertThrows<DomainException> {
            useCase.execute(DeleteFolderInput(userId = userId, folderId = parentFolder.id))
        }

        assertEquals(ErrorCode.FOLDER_HAS_CHILDREN, exception.code)

        // フォルダーは削除されていないことを確認
        assertEquals(parentFolder, folderRepository.findById(parentFolder.id))
        assertEquals(childFolder, folderRepository.findById(childFolder.id))
    }

    @Test
    fun `メモが存在する場合は削除できない`() = runTest {
        val userId = UUID.randomUUID()
        val folder = Folder.create(
            id = UUID.randomUUID(),
            userId = userId,
            name = "仕事",
            parentId = null,
        )
        val memo = VoiceMemo.restore(
            id = UUID.randomUUID(),
            userId = userId,
            transcription = Transcription.completed("テスト", "ja-JP"),
            formatting = Formatting.completed(
                title = "テストメモ",
                content = "テスト内容",
                tagIds = emptyList(),
                folderId = folder.id,
            ),
            deleted = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )

        val folderRepository = InMemoryFolderRepository(initialFolders = listOf(folder))
        val voiceMemoRepository = InMemoryVoiceMemoRepository(initialMemos = listOf(memo))
        val useCase = DeleteFolderUseCase(folderRepository, voiceMemoRepository)

        val exception = assertThrows<DomainException> {
            useCase.execute(DeleteFolderInput(userId = userId, folderId = folder.id))
        }

        assertEquals(ErrorCode.FOLDER_HAS_MEMOS, exception.code)

        // フォルダーは削除されていないことを確認
        assertEquals(folder, folderRepository.findById(folder.id))
    }

    @Test
    fun `子フォルダーとメモの両方が存在する場合は子フォルダーのエラーが優先される`() = runTest {
        val userId = UUID.randomUUID()
        val parentFolder = Folder.create(
            id = UUID.randomUUID(),
            userId = userId,
            name = "仕事",
            parentId = null,
        )
        val childFolder = Folder.create(
            id = UUID.randomUUID(),
            userId = userId,
            name = "プロジェクトA",
            parentId = parentFolder.id,
        )
        val memo = VoiceMemo.restore(
            id = UUID.randomUUID(),
            userId = userId,
            transcription = Transcription.completed("テスト", "ja-JP"),
            formatting = Formatting.completed(
                title = "テストメモ",
                content = "テスト内容",
                tagIds = emptyList(),
                folderId = parentFolder.id,
            ),
            deleted = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )

        val folderRepository = InMemoryFolderRepository(initialFolders = listOf(parentFolder, childFolder))
        val voiceMemoRepository = InMemoryVoiceMemoRepository(initialMemos = listOf(memo))
        val useCase = DeleteFolderUseCase(folderRepository, voiceMemoRepository)

        val exception = assertThrows<DomainException> {
            useCase.execute(DeleteFolderInput(userId = userId, folderId = parentFolder.id))
        }

        // 子フォルダーのチェックが先に行われるため、FOLDER_HAS_CHILDRENエラーになる
        assertEquals(ErrorCode.FOLDER_HAS_CHILDREN, exception.code)
    }
}

