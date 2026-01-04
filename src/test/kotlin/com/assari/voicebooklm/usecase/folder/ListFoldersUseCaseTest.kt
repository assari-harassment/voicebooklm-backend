package com.assari.voicebooklm.usecase.folder

import com.assari.voicebooklm.domain.model.Folder
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ListFoldersUseCase の振る舞いをテストダブルで検証。
 */
class ListFoldersUseCaseTest {

    @Test
    fun `ユーザーのフォルダー一覧を取得できる`() = runTest {
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
        val folderRepository = InMemoryFolderRepository(
            initialFolders = listOf(folder1, folder2)
        )
        val useCase = ListFoldersUseCase(folderRepository)

        val result = useCase.execute(ListFoldersInput(userId = userId))

        assertEquals(2, result.folders.size)
    }

    @Test
    fun `フォルダーが0件の場合は空の一覧を返す`() = runTest {
        val folderRepository = InMemoryFolderRepository()
        val useCase = ListFoldersUseCase(folderRepository)

        val result = useCase.execute(ListFoldersInput(userId = UUID.randomUUID()))

        assertTrue(result.folders.isEmpty())
    }

    @Test
    fun `他ユーザーのフォルダーは含まれない`() = runTest {
        val userId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        val userFolder = Folder.create(
            id = UUID.randomUUID(),
            userId = userId,
            name = "自分のフォルダー",
            parentId = null,
        )
        val otherUserFolder = Folder.create(
            id = UUID.randomUUID(),
            userId = otherUserId,
            name = "他人のフォルダー",
            parentId = null,
        )
        val folderRepository = InMemoryFolderRepository(
            initialFolders = listOf(userFolder, otherUserFolder)
        )
        val useCase = ListFoldersUseCase(folderRepository)

        val result = useCase.execute(ListFoldersInput(userId = userId))

        assertEquals(1, result.folders.size)
        assertEquals("自分のフォルダー", result.folders[0].folder.name)
    }

    @Test
    fun `階層構造のパスが正しく構築される`() = runTest {
        val userId = UUID.randomUUID()
        val rootFolder = Folder.create(
            id = UUID.randomUUID(),
            userId = userId,
            name = "仕事",
            parentId = null,
        )
        val childFolder = Folder.create(
            id = UUID.randomUUID(),
            userId = userId,
            name = "プロジェクトA",
            parentId = rootFolder.id,
        )
        val grandchildFolder = Folder.create(
            id = UUID.randomUUID(),
            userId = userId,
            name = "設計",
            parentId = childFolder.id,
        )
        val folderRepository = InMemoryFolderRepository(
            initialFolders = listOf(rootFolder, childFolder, grandchildFolder)
        )
        val useCase = ListFoldersUseCase(folderRepository)

        val result = useCase.execute(ListFoldersInput(userId = userId))

        assertEquals(3, result.folders.size)
        // パスでソートされる
        val paths = result.folders.map { it.path }
        assertEquals("仕事", paths[0])
        assertEquals("仕事/プロジェクトA", paths[1])
        assertEquals("仕事/プロジェクトA/設計", paths[2])
    }

    @Test
    fun `複数のルートフォルダーがある場合も正しく取得できる`() = runTest {
        val userId = UUID.randomUUID()
        val workFolder = Folder.create(
            id = UUID.randomUUID(),
            userId = userId,
            name = "仕事",
            parentId = null,
        )
        val privateFolder = Folder.create(
            id = UUID.randomUUID(),
            userId = userId,
            name = "プライベート",
            parentId = null,
        )
        val workChild = Folder.create(
            id = UUID.randomUUID(),
            userId = userId,
            name = "会議",
            parentId = workFolder.id,
        )
        val privateChild = Folder.create(
            id = UUID.randomUUID(),
            userId = userId,
            name = "旅行",
            parentId = privateFolder.id,
        )
        val folderRepository = InMemoryFolderRepository(
            initialFolders = listOf(workFolder, privateFolder, workChild, privateChild)
        )
        val useCase = ListFoldersUseCase(folderRepository)

        val result = useCase.execute(ListFoldersInput(userId = userId))

        assertEquals(4, result.folders.size)
        val paths = result.folders.map { it.path }
        // パスでソートされる
        assertTrue(paths.contains("仕事"))
        assertTrue(paths.contains("仕事/会議"))
        assertTrue(paths.contains("プライベート"))
        assertTrue(paths.contains("プライベート/旅行"))
    }
}
