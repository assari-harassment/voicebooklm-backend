package com.assari.voicebooklm.usecase.folder

import com.assari.voicebooklm.domain.exception.DomainException
import com.assari.voicebooklm.domain.exception.ErrorCode
import com.assari.voicebooklm.domain.model.Folder
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * CreateFolderUseCase の振る舞いをテストダブルで検証。
 */
class CreateFolderUseCaseTest {

    @Test
    fun `ルートフォルダーを作成できる`() = runTest {
        val userId = UUID.randomUUID()
        val folderRepository = InMemoryFolderRepository()
        val useCase = CreateFolderUseCase(folderRepository)

        val result = useCase.execute(
            CreateFolderInput(
                userId = userId,
                name = "仕事",
                parentId = null,
            )
        )

        assertNotNull(result.folder.id)
        assertEquals("仕事", result.folder.name)
        assertEquals(userId, result.folder.userId)
        assertEquals(null, result.folder.parentId)
        assertEquals("仕事", result.path)
    }

    @Test
    fun `子フォルダーを作成できる`() = runTest {
        val userId = UUID.randomUUID()
        val parentFolder = Folder.create(
            id = UUID.randomUUID(),
            userId = userId,
            name = "仕事",
            parentId = null,
        )
        val folderRepository = InMemoryFolderRepository(initialFolders = listOf(parentFolder))
        val useCase = CreateFolderUseCase(folderRepository)

        val result = useCase.execute(
            CreateFolderInput(
                userId = userId,
                name = "プロジェクトA",
                parentId = parentFolder.id,
            )
        )

        assertNotNull(result.folder.id)
        assertEquals("プロジェクトA", result.folder.name)
        assertEquals(userId, result.folder.userId)
        assertEquals(parentFolder.id, result.folder.parentId)
        assertEquals("仕事/プロジェクトA", result.path)
    }

    @Test
    fun `存在しない親フォルダーを指定するとエラーになる`() = runTest {
        val userId = UUID.randomUUID()
        val folderRepository = InMemoryFolderRepository()
        val useCase = CreateFolderUseCase(folderRepository)

        val exception = assertThrows<DomainException> {
            useCase.execute(
                CreateFolderInput(
                    userId = userId,
                    name = "プロジェクトA",
                    parentId = UUID.randomUUID(), // 存在しないID
                )
            )
        }

        assertEquals(ErrorCode.FOLDER_NOT_FOUND, exception.code)
    }

    @Test
    fun `他ユーザーの親フォルダーを指定するとエラーになる`() = runTest {
        val userId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        val otherUserFolder = Folder.create(
            id = UUID.randomUUID(),
            userId = otherUserId,
            name = "他人のフォルダー",
            parentId = null,
        )
        val folderRepository = InMemoryFolderRepository(initialFolders = listOf(otherUserFolder))
        val useCase = CreateFolderUseCase(folderRepository)

        val exception = assertThrows<DomainException> {
            useCase.execute(
                CreateFolderInput(
                    userId = userId,
                    name = "プロジェクト",
                    parentId = otherUserFolder.id,
                )
            )
        }

        assertEquals(ErrorCode.FOLDER_NOT_FOUND, exception.code)
    }

    @Test
    fun `同じ親フォルダー内に同名フォルダーが存在するとエラーになる`() = runTest {
        val userId = UUID.randomUUID()
        val existingFolder = Folder.create(
            id = UUID.randomUUID(),
            userId = userId,
            name = "仕事",
            parentId = null,
        )
        val folderRepository = InMemoryFolderRepository(initialFolders = listOf(existingFolder))
        val useCase = CreateFolderUseCase(folderRepository)

        val exception = assertThrows<DomainException> {
            useCase.execute(
                CreateFolderInput(
                    userId = userId,
                    name = "仕事", // 同名
                    parentId = null,
                )
            )
        }

        assertEquals(ErrorCode.FOLDER_ALREADY_EXISTS, exception.code)
    }

    @Test
    fun `異なる親フォルダーには同名フォルダーを作成できる`() = runTest {
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
        val existingChild = Folder.create(
            id = UUID.randomUUID(),
            userId = userId,
            name = "ミーティング",
            parentId = parentFolder1.id,
        )
        val folderRepository = InMemoryFolderRepository(
            initialFolders = listOf(parentFolder1, parentFolder2, existingChild)
        )
        val useCase = CreateFolderUseCase(folderRepository)

        // 別の親フォルダーに同名フォルダーを作成
        val result = useCase.execute(
            CreateFolderInput(
                userId = userId,
                name = "ミーティング",
                parentId = parentFolder2.id,
            )
        )

        assertEquals("ミーティング", result.folder.name)
        assertEquals(parentFolder2.id, result.folder.parentId)
    }

    @Test
    fun `フォルダー名の前後の空白はトリムされる`() = runTest {
        val userId = UUID.randomUUID()
        val folderRepository = InMemoryFolderRepository()
        val useCase = CreateFolderUseCase(folderRepository)

        val result = useCase.execute(
            CreateFolderInput(
                userId = userId,
                name = "  仕事  ",
                parentId = null,
            )
        )

        assertEquals("仕事", result.folder.name)
    }
}
