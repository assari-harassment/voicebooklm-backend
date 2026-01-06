package com.assari.voicebooklm.usecase.folder

import com.assari.voicebooklm.domain.exception.DomainException
import com.assari.voicebooklm.domain.exception.ErrorCode
import com.assari.voicebooklm.domain.model.Folder
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * UpdateFolderUseCase の振る舞いをテストダブルで検証。
 */
class UpdateFolderUseCaseTest {

    // ===========================================
    // リネームのテスト
    // ===========================================

    @Test
    fun `フォルダー名を変更できる`() = runTest {
        val userId = UUID.randomUUID()
        val folder = Folder.create(
            id = UUID.randomUUID(),
            userId = userId,
            name = "仕事",
            parentId = null,
        )
        val folderRepository = InMemoryFolderRepository(initialFolders = listOf(folder))
        val useCase = UpdateFolderUseCase(folderRepository)

        val result = useCase.execute(
            UpdateFolderInput(
                userId = userId,
                folderId = folder.id,
                newName = "Work",
            )
        )

        assertEquals("Work", result.folder.name)
        assertEquals(folder.id, result.folder.id)
    }

    @Test
    fun `同名フォルダーが既に存在する場合はリネームできない`() = runTest {
        val userId = UUID.randomUUID()
        val folder1 = Folder.create(
            id = UUID.randomUUID(),
            userId = userId,
            name = "仕事",
            parentId = null,
        )
        val folder2 = Folder.create(
            id = UUID.randomUUID(),
            userId = userId,
            name = "プライベート",
            parentId = null,
        )
        val folderRepository = InMemoryFolderRepository(initialFolders = listOf(folder1, folder2))
        val useCase = UpdateFolderUseCase(folderRepository)

        val exception = assertThrows<DomainException> {
            useCase.execute(
                UpdateFolderInput(
                    userId = userId,
                    folderId = folder2.id,
                    newName = "仕事", // folder1と同名
                )
            )
        }

        assertEquals(ErrorCode.FOLDER_ALREADY_EXISTS, exception.code)
    }

    // ===========================================
    // 移動のテスト
    // ===========================================

    @Test
    fun `フォルダーを別の親フォルダーに移動できる`() = runTest {
        val userId = UUID.randomUUID()
        val parentFolder1 = Folder.create(
            id = UUID.randomUUID(),
            userId = userId,
            name = "仕事",
            parentId = null,
        )
        val parentFolder2 = Folder.create(
            id = UUID.randomUUID(),
            userId = userId,
            name = "プライベート",
            parentId = null,
        )
        val childFolder = Folder.create(
            id = UUID.randomUUID(),
            userId = userId,
            name = "プロジェクトA",
            parentId = parentFolder1.id,
        )
        val folderRepository = InMemoryFolderRepository(
            initialFolders = listOf(parentFolder1, parentFolder2, childFolder)
        )
        val useCase = UpdateFolderUseCase(folderRepository)

        val result = useCase.execute(
            UpdateFolderInput(
                userId = userId,
                folderId = childFolder.id,
                newParentId = parentFolder2.id,
            )
        )

        assertEquals(parentFolder2.id, result.folder.parentId)
        assertEquals("プロジェクトA", result.folder.name)
    }

    @Test
    fun `フォルダーをルートに移動できる`() = runTest {
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
        val folderRepository = InMemoryFolderRepository(
            initialFolders = listOf(parentFolder, childFolder)
        )
        val useCase = UpdateFolderUseCase(folderRepository)

        val result = useCase.execute(
            UpdateFolderInput(
                userId = userId,
                folderId = childFolder.id,
                moveToRoot = true,
            )
        )

        assertEquals(null, result.folder.parentId)
        assertEquals("プロジェクトA", result.folder.name)
    }

    @Test
    fun `自分自身の子孫フォルダーには移動できない（循環参照防止）`() = runTest {
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
        val grandchildFolder = Folder.create(
            id = UUID.randomUUID(),
            userId = userId,
            name = "設計",
            parentId = childFolder.id,
        )
        val folderRepository = InMemoryFolderRepository(
            initialFolders = listOf(parentFolder, childFolder, grandchildFolder)
        )
        val useCase = UpdateFolderUseCase(folderRepository)

        val exception = assertThrows<DomainException> {
            useCase.execute(
                UpdateFolderInput(
                    userId = userId,
                    folderId = parentFolder.id,
                    newParentId = grandchildFolder.id, // 自分の孫フォルダーに移動しようとする
                )
            )
        }

        assertEquals(ErrorCode.FOLDER_CIRCULAR_REFERENCE, exception.code)
    }

    @Test
    fun `存在しない移動先フォルダーを指定するとエラーになる`() = runTest {
        val userId = UUID.randomUUID()
        val folder = Folder.create(
            id = UUID.randomUUID(),
            userId = userId,
            name = "仕事",
            parentId = null,
        )
        val folderRepository = InMemoryFolderRepository(initialFolders = listOf(folder))
        val useCase = UpdateFolderUseCase(folderRepository)

        val exception = assertThrows<DomainException> {
            useCase.execute(
                UpdateFolderInput(
                    userId = userId,
                    folderId = folder.id,
                    newParentId = UUID.randomUUID(), // 存在しないID
                )
            )
        }

        assertEquals(ErrorCode.FOLDER_NOT_FOUND, exception.code)
    }

    @Test
    fun `他ユーザーの移動先フォルダーを指定するとエラーになる`() = runTest {
        val userId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        val folder = Folder.create(
            id = UUID.randomUUID(),
            userId = userId,
            name = "仕事",
            parentId = null,
        )
        val otherUserFolder = Folder.create(
            id = UUID.randomUUID(),
            userId = otherUserId,
            name = "他人のフォルダー",
            parentId = null,
        )
        val folderRepository = InMemoryFolderRepository(
            initialFolders = listOf(folder, otherUserFolder)
        )
        val useCase = UpdateFolderUseCase(folderRepository)

        val exception = assertThrows<DomainException> {
            useCase.execute(
                UpdateFolderInput(
                    userId = userId,
                    folderId = folder.id,
                    newParentId = otherUserFolder.id,
                )
            )
        }

        assertEquals(ErrorCode.FOLDER_NOT_FOUND, exception.code)
    }

    // ===========================================
    // リネーム＋移動の同時実行テスト
    // ===========================================

    @Test
    fun `リネームと移動を同時に実行できる`() = runTest {
        val userId = UUID.randomUUID()
        val parentFolder1 = Folder.create(
            id = UUID.randomUUID(),
            userId = userId,
            name = "仕事",
            parentId = null,
        )
        val parentFolder2 = Folder.create(
            id = UUID.randomUUID(),
            userId = userId,
            name = "プライベート",
            parentId = null,
        )
        val childFolder = Folder.create(
            id = UUID.randomUUID(),
            userId = userId,
            name = "プロジェクトA",
            parentId = parentFolder1.id,
        )
        val folderRepository = InMemoryFolderRepository(
            initialFolders = listOf(parentFolder1, parentFolder2, childFolder)
        )
        val useCase = UpdateFolderUseCase(folderRepository)

        val result = useCase.execute(
            UpdateFolderInput(
                userId = userId,
                folderId = childFolder.id,
                newName = "趣味プロジェクト",
                newParentId = parentFolder2.id,
            )
        )

        assertEquals("趣味プロジェクト", result.folder.name)
        assertEquals(parentFolder2.id, result.folder.parentId)
    }

    // ===========================================
    // その他のテスト
    // ===========================================

    @Test
    fun `存在しないフォルダーを更新しようとするとエラーになる`() = runTest {
        val userId = UUID.randomUUID()
        val folderRepository = InMemoryFolderRepository()
        val useCase = UpdateFolderUseCase(folderRepository)

        val exception = assertThrows<DomainException> {
            useCase.execute(
                UpdateFolderInput(
                    userId = userId,
                    folderId = UUID.randomUUID(),
                    newName = "新しい名前",
                )
            )
        }

        assertEquals(ErrorCode.FOLDER_NOT_FOUND, exception.code)
    }

    @Test
    fun `他ユーザーのフォルダーを更新しようとするとエラーになる`() = runTest {
        val userId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        val otherUserFolder = Folder.create(
            id = UUID.randomUUID(),
            userId = otherUserId,
            name = "他人のフォルダー",
            parentId = null,
        )
        val folderRepository = InMemoryFolderRepository(initialFolders = listOf(otherUserFolder))
        val useCase = UpdateFolderUseCase(folderRepository)

        val exception = assertThrows<DomainException> {
            useCase.execute(
                UpdateFolderInput(
                    userId = userId,
                    folderId = otherUserFolder.id,
                    newName = "新しい名前",
                )
            )
        }

        assertEquals(ErrorCode.FOLDER_NOT_FOUND, exception.code)
    }

    @Test
    fun `変更がない場合はそのまま返す`() = runTest {
        val userId = UUID.randomUUID()
        val folder = Folder.create(
            id = UUID.randomUUID(),
            userId = userId,
            name = "仕事",
            parentId = null,
        )
        val folderRepository = InMemoryFolderRepository(initialFolders = listOf(folder))
        val useCase = UpdateFolderUseCase(folderRepository)

        val result = useCase.execute(
            UpdateFolderInput(
                userId = userId,
                folderId = folder.id,
                newName = null,
                newParentId = null,
                moveToRoot = false,
            )
        )

        assertEquals(folder.id, result.folder.id)
        assertEquals("仕事", result.folder.name)
    }
}
