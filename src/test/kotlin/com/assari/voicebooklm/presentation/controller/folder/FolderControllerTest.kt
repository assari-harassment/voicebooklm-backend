package com.assari.voicebooklm.presentation.controller.folder

import com.assari.voicebooklm.domain.exception.DomainException
import com.assari.voicebooklm.domain.exception.ErrorCode
import com.assari.voicebooklm.domain.model.Folder
import com.assari.voicebooklm.usecase.folder.CreateFolderInput
import com.assari.voicebooklm.usecase.folder.CreateFolderOutput
import com.assari.voicebooklm.usecase.folder.DeleteFolderInput
import com.assari.voicebooklm.usecase.folder.DeleteFolderUseCase
import com.assari.voicebooklm.usecase.folder.FolderWithPath
import com.assari.voicebooklm.usecase.folder.ListFoldersInput
import com.assari.voicebooklm.usecase.folder.ListFoldersOutput
import com.assari.voicebooklm.usecase.folder.ListFoldersUseCase
import com.assari.voicebooklm.usecase.folder.UpdateFolderInput
import com.assari.voicebooklm.usecase.folder.UpdateFolderOutput
import com.assari.voicebooklm.usecase.folder.UpdateFolderUseCase
import com.assari.voicebooklm.usecase.folder.CreateFolderUseCase
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.mockk
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

/**
 * FolderController の一覧取得、作成、更新、削除を直接呼び出しで検証。
 */
class FolderControllerTest {

    private lateinit var listFoldersUseCase: ListFoldersUseCase
    private lateinit var createFolderUseCase: CreateFolderUseCase
    private lateinit var updateFolderUseCase: UpdateFolderUseCase
    private lateinit var deleteFolderUseCase: DeleteFolderUseCase
    private lateinit var controller: FolderController

    @BeforeEach
    fun setup() {
        listFoldersUseCase = mockk()
        createFolderUseCase = mockk()
        updateFolderUseCase = mockk()
        deleteFolderUseCase = mockk()
        controller = FolderController(
            listFoldersUseCase = listFoldersUseCase,
            createFolderUseCase = createFolderUseCase,
            updateFolderUseCase = updateFolderUseCase,
            deleteFolderUseCase = deleteFolderUseCase,
        )
    }

    // ===== listFolders エンドポイントのテスト =====

    @Test
    fun `認証済みユーザーのフォルダー一覧を返す`() = runBlocking {
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
        val input = ListFoldersInput(userId = userId)
        coEvery { listFoldersUseCase.execute(input) } returns ListFoldersOutput(
            folders = listOf(
                FolderWithPath(folder = folder1, path = "仕事"),
                FolderWithPath(folder = folder2, path = "プライベート"),
            ),
        )

        val response = controller.listFolders(userId)

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = requireNotNull(response.body) { "response body should not be null" }
        assertEquals(2, body.folders.size)
        assertEquals(folder1.id, body.folders[0].id)
        assertEquals("仕事", body.folders[0].name)
        assertEquals("仕事", body.folders[0].path)
        assertEquals(folder2.id, body.folders[1].id)
        assertEquals("プライベート", body.folders[1].name)
        assertEquals("プライベート", body.folders[1].path)
    }

    @Test
    fun `階層構造のフォルダーが正しくパス情報付きで返される`() = runBlocking {
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
        val input = ListFoldersInput(userId = userId)
        coEvery { listFoldersUseCase.execute(input) } returns ListFoldersOutput(
            folders = listOf(
                FolderWithPath(folder = parentFolder, path = "仕事"),
                FolderWithPath(folder = childFolder, path = "仕事/プロジェクトA"),
            ),
        )

        val response = controller.listFolders(userId)

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = requireNotNull(response.body) { "response body should not be null" }
        assertEquals(2, body.folders.size)
        assertEquals("仕事", body.folders[0].path)
        assertEquals("仕事/プロジェクトA", body.folders[1].path)
        assertEquals(parentFolder.id, body.folders[1].parentId)
    }

    @Test
    fun `フォルダーが存在しない場合は空リストを返す`() = runBlocking {
        val userId = UUID.randomUUID()
        val input = ListFoldersInput(userId = userId)
        coEvery { listFoldersUseCase.execute(input) } returns ListFoldersOutput(
            folders = emptyList(),
        )

        val response = controller.listFolders(userId)

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = requireNotNull(response.body) { "response body should not be null" }
        assertTrue(body.folders.isEmpty())
    }

    @Test
    fun `listFolders 未認証の場合はResponseStatusExceptionがスローされる`() = runBlocking {
        val exception = assertThrows<ResponseStatusException> {
            controller.listFolders(null)
        }

        assertEquals(HttpStatus.UNAUTHORIZED, exception.statusCode)
    }

    // ===== createFolder エンドポイントのテスト =====

    @Test
    fun `認証済みユーザーがルートフォルダーを作成すると201が返る`() = runBlocking {
        val userId = UUID.randomUUID()
        val folder = Folder.create(
            id = UUID.randomUUID(),
            userId = userId,
            name = "仕事",
            parentId = null,
        )
        val request = CreateFolderRequest(name = "仕事", parentId = null)
        val input = CreateFolderInput(
            userId = userId,
            name = "仕事",
            parentId = null,
        )
        coEvery { createFolderUseCase.execute(input) } returns CreateFolderOutput(
            folder = folder,
            path = "仕事",
        )

        val response = controller.createFolder(userId, request)

        assertEquals(HttpStatus.CREATED, response.statusCode)
        val body = requireNotNull(response.body) { "response body should not be null" }
        assertEquals(folder.id, body.id)
        assertEquals("仕事", body.name)
        assertNull(body.parentId)
        assertEquals("仕事", body.path)
    }

    @Test
    fun `認証済みユーザーが子フォルダーを作成すると201が返る`() = runBlocking {
        val userId = UUID.randomUUID()
        val parentId = UUID.randomUUID()
        val folder = Folder.create(
            id = UUID.randomUUID(),
            userId = userId,
            name = "プロジェクトA",
            parentId = parentId,
        )
        val request = CreateFolderRequest(name = "プロジェクトA", parentId = parentId)
        val input = CreateFolderInput(
            userId = userId,
            name = "プロジェクトA",
            parentId = parentId,
        )
        coEvery { createFolderUseCase.execute(input) } returns CreateFolderOutput(
            folder = folder,
            path = "仕事/プロジェクトA",
        )

        val response = controller.createFolder(userId, request)

        assertEquals(HttpStatus.CREATED, response.statusCode)
        val body = requireNotNull(response.body) { "response body should not be null" }
        assertEquals(folder.id, body.id)
        assertEquals("プロジェクトA", body.name)
        assertEquals(parentId, body.parentId)
        assertEquals("仕事/プロジェクトA", body.path)
    }

    @Test
    fun `createFolder 未認証の場合はResponseStatusExceptionがスローされる`() = runBlocking {
        val request = CreateFolderRequest(name = "仕事")

        val exception = assertThrows<ResponseStatusException> {
            controller.createFolder(null, request)
        }

        assertEquals(HttpStatus.UNAUTHORIZED, exception.statusCode)
    }

    @Test
    fun `createFolder 存在しない親フォルダーを指定するとFOLDER_NOT_FOUND例外がスローされる`() = runBlocking {
        val userId = UUID.randomUUID()
        val parentId = UUID.randomUUID()
        val request = CreateFolderRequest(name = "プロジェクトA", parentId = parentId)
        val input = CreateFolderInput(
            userId = userId,
            name = "プロジェクトA",
            parentId = parentId,
        )
        coEvery { createFolderUseCase.execute(input) } throws
            DomainException(ErrorCode.FOLDER_NOT_FOUND)

        val exception = assertThrows<DomainException> {
            controller.createFolder(userId, request)
        }

        assertEquals(ErrorCode.FOLDER_NOT_FOUND, exception.code)
    }

    @Test
    fun `createFolder 同名フォルダーが既に存在するとFOLDER_ALREADY_EXISTS例外がスローされる`() = runBlocking {
        val userId = UUID.randomUUID()
        val request = CreateFolderRequest(name = "仕事", parentId = null)
        val input = CreateFolderInput(
            userId = userId,
            name = "仕事",
            parentId = null,
        )
        coEvery { createFolderUseCase.execute(input) } throws
            DomainException(ErrorCode.FOLDER_ALREADY_EXISTS)

        val exception = assertThrows<DomainException> {
            controller.createFolder(userId, request)
        }

        assertEquals(ErrorCode.FOLDER_ALREADY_EXISTS, exception.code)
    }

    // ===== updateFolder エンドポイントのテスト =====

    @Test
    fun `認証済みユーザーが自分のフォルダーをリネームすると200が返る`() = runBlocking {
        val userId = UUID.randomUUID()
        val folderId = UUID.randomUUID()
        val updatedFolder = Folder.create(
            id = folderId,
            userId = userId,
            name = "Work",
            parentId = null,
        )
        val request = UpdateFolderRequest(name = "Work", parentId = null, moveToRoot = false)
        val input = UpdateFolderInput(
            userId = userId,
            folderId = folderId,
            newName = "Work",
            newParentId = null,
            moveToRoot = false,
        )
        coEvery { updateFolderUseCase.execute(input) } returns UpdateFolderOutput(
            folder = updatedFolder,
            path = "Work",
        )

        val response = controller.updateFolder(userId, folderId, request)

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = requireNotNull(response.body) { "response body should not be null" }
        assertEquals(folderId, body.id)
        assertEquals("Work", body.name)
        assertEquals("Work", body.path)
    }

    @Test
    fun `認証済みユーザーが自分のフォルダーを移動すると200が返る`() = runBlocking {
        val userId = UUID.randomUUID()
        val folderId = UUID.randomUUID()
        val newParentId = UUID.randomUUID()
        val updatedFolder = Folder.create(
            id = folderId,
            userId = userId,
            name = "プロジェクトA",
            parentId = newParentId,
        )
        val request = UpdateFolderRequest(name = null, parentId = newParentId, moveToRoot = false)
        val input = UpdateFolderInput(
            userId = userId,
            folderId = folderId,
            newName = null,
            newParentId = newParentId,
            moveToRoot = false,
        )
        coEvery { updateFolderUseCase.execute(input) } returns UpdateFolderOutput(
            folder = updatedFolder,
            path = "プライベート/プロジェクトA",
        )

        val response = controller.updateFolder(userId, folderId, request)

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = requireNotNull(response.body) { "response body should not be null" }
        assertEquals(folderId, body.id)
        assertEquals(newParentId, body.parentId)
        assertEquals("プライベート/プロジェクトA", body.path)
    }

    @Test
    fun `認証済みユーザーが自分のフォルダーをルートに移動すると200が返る`() = runBlocking {
        val userId = UUID.randomUUID()
        val folderId = UUID.randomUUID()
        val updatedFolder = Folder.create(
            id = folderId,
            userId = userId,
            name = "プロジェクトA",
            parentId = null,
        )
        val request = UpdateFolderRequest(name = null, parentId = null, moveToRoot = true)
        val input = UpdateFolderInput(
            userId = userId,
            folderId = folderId,
            newName = null,
            newParentId = null,
            moveToRoot = true,
        )
        coEvery { updateFolderUseCase.execute(input) } returns UpdateFolderOutput(
            folder = updatedFolder,
            path = "プロジェクトA",
        )

        val response = controller.updateFolder(userId, folderId, request)

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = requireNotNull(response.body) { "response body should not be null" }
        assertEquals(folderId, body.id)
        assertNull(body.parentId)
        assertEquals("プロジェクトA", body.path)
    }

    @Test
    fun `認証済みユーザーが自分のフォルダーをリネームと移動を同時に実行すると200が返る`() = runBlocking {
        val userId = UUID.randomUUID()
        val folderId = UUID.randomUUID()
        val newParentId = UUID.randomUUID()
        val updatedFolder = Folder.create(
            id = folderId,
            userId = userId,
            name = "趣味プロジェクト",
            parentId = newParentId,
        )
        val request = UpdateFolderRequest(
            name = "趣味プロジェクト",
            parentId = newParentId,
            moveToRoot = false,
        )
        val input = UpdateFolderInput(
            userId = userId,
            folderId = folderId,
            newName = "趣味プロジェクト",
            newParentId = newParentId,
            moveToRoot = false,
        )
        coEvery { updateFolderUseCase.execute(input) } returns UpdateFolderOutput(
            folder = updatedFolder,
            path = "プライベート/趣味プロジェクト",
        )

        val response = controller.updateFolder(userId, folderId, request)

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = requireNotNull(response.body) { "response body should not be null" }
        assertEquals(folderId, body.id)
        assertEquals("趣味プロジェクト", body.name)
        assertEquals(newParentId, body.parentId)
        assertEquals("プライベート/趣味プロジェクト", body.path)
    }

    @Test
    fun `updateFolder 未認証の場合はResponseStatusExceptionがスローされる`() = runBlocking {
        val folderId = UUID.randomUUID()
        val request = UpdateFolderRequest(name = "新しい名前")

        val exception = assertThrows<ResponseStatusException> {
            controller.updateFolder(null, folderId, request)
        }

        assertEquals(HttpStatus.UNAUTHORIZED, exception.statusCode)
    }

    @Test
    fun `updateFolder 存在しないフォルダーIDを指定するとFOLDER_NOT_FOUND例外がスローされる`() = runBlocking {
        val userId = UUID.randomUUID()
        val folderId = UUID.randomUUID()
        val request = UpdateFolderRequest(name = "新しい名前")
        val input = UpdateFolderInput(
            userId = userId,
            folderId = folderId,
            newName = "新しい名前",
            newParentId = null,
            moveToRoot = false,
        )
        coEvery { updateFolderUseCase.execute(input) } throws
            DomainException(ErrorCode.FOLDER_NOT_FOUND)

        val exception = assertThrows<DomainException> {
            controller.updateFolder(userId, folderId, request)
        }

        assertEquals(ErrorCode.FOLDER_NOT_FOUND, exception.code)
    }

    @Test
    fun `updateFolder 他人のフォルダーを更新しようとするとFOLDER_NOT_FOUND例外がスローされる`() = runBlocking {
        val userId = UUID.randomUUID()
        val folderId = UUID.randomUUID()
        val request = UpdateFolderRequest(name = "新しい名前")
        val input = UpdateFolderInput(
            userId = userId,
            folderId = folderId,
            newName = "新しい名前",
            newParentId = null,
            moveToRoot = false,
        )
        coEvery { updateFolderUseCase.execute(input) } throws
            DomainException(ErrorCode.FOLDER_NOT_FOUND)

        val exception = assertThrows<DomainException> {
            controller.updateFolder(userId, folderId, request)
        }

        assertEquals(ErrorCode.FOLDER_NOT_FOUND, exception.code)
    }

    @Test
    fun `updateFolder 同名フォルダーが既に存在するとFOLDER_ALREADY_EXISTS例外がスローされる`() = runBlocking {
        val userId = UUID.randomUUID()
        val folderId = UUID.randomUUID()
        val request = UpdateFolderRequest(name = "仕事")
        val input = UpdateFolderInput(
            userId = userId,
            folderId = folderId,
            newName = "仕事",
            newParentId = null,
            moveToRoot = false,
        )
        coEvery { updateFolderUseCase.execute(input) } throws
            DomainException(ErrorCode.FOLDER_ALREADY_EXISTS)

        val exception = assertThrows<DomainException> {
            controller.updateFolder(userId, folderId, request)
        }

        assertEquals(ErrorCode.FOLDER_ALREADY_EXISTS, exception.code)
    }

    @Test
    fun `updateFolder 循環参照が発生するとFOLDER_CIRCULAR_REFERENCE例外がスローされる`() = runBlocking {
        val userId = UUID.randomUUID()
        val folderId = UUID.randomUUID()
        val newParentId = UUID.randomUUID()
        val request = UpdateFolderRequest(name = null, parentId = newParentId, moveToRoot = false)
        val input = UpdateFolderInput(
            userId = userId,
            folderId = folderId,
            newName = null,
            newParentId = newParentId,
            moveToRoot = false,
        )
        coEvery { updateFolderUseCase.execute(input) } throws
            DomainException(ErrorCode.FOLDER_CIRCULAR_REFERENCE)

        val exception = assertThrows<DomainException> {
            controller.updateFolder(userId, folderId, request)
        }

        assertEquals(ErrorCode.FOLDER_CIRCULAR_REFERENCE, exception.code)
    }

    // ===== deleteFolder エンドポイントのテスト =====

    @Test
    fun `認証済みユーザーが自分のフォルダーを削除すると204が返る`() = runBlocking {
        val userId = UUID.randomUUID()
        val folderId = UUID.randomUUID()
        val input = DeleteFolderInput(userId = userId, folderId = folderId)
        coJustRun { deleteFolderUseCase.execute(input) }

        val response = controller.deleteFolder(userId, folderId)

        assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
        assertNull(response.body)
    }

    @Test
    fun `deleteFolder 未認証の場合はResponseStatusExceptionがスローされる`() = runBlocking {
        val folderId = UUID.randomUUID()

        val exception = assertThrows<ResponseStatusException> {
            controller.deleteFolder(null, folderId)
        }

        assertEquals(HttpStatus.UNAUTHORIZED, exception.statusCode)
    }

    @Test
    fun `deleteFolder 存在しないフォルダーIDを指定するとFOLDER_NOT_FOUND例外がスローされる`() = runBlocking {
        val userId = UUID.randomUUID()
        val folderId = UUID.randomUUID()
        val input = DeleteFolderInput(userId = userId, folderId = folderId)
        coEvery { deleteFolderUseCase.execute(input) } throws
            DomainException(ErrorCode.FOLDER_NOT_FOUND)

        val exception = assertThrows<DomainException> {
            controller.deleteFolder(userId, folderId)
        }

        assertEquals(ErrorCode.FOLDER_NOT_FOUND, exception.code)
    }

    @Test
    fun `deleteFolder 他人のフォルダーを削除しようとするとFOLDER_NOT_FOUND例外がスローされる`() = runBlocking {
        val userId = UUID.randomUUID()
        val folderId = UUID.randomUUID()
        val input = DeleteFolderInput(userId = userId, folderId = folderId)
        coEvery { deleteFolderUseCase.execute(input) } throws
            DomainException(ErrorCode.FOLDER_NOT_FOUND)

        val exception = assertThrows<DomainException> {
            controller.deleteFolder(userId, folderId)
        }

        assertEquals(ErrorCode.FOLDER_NOT_FOUND, exception.code)
    }

    @Test
    fun `deleteFolder 子フォルダーが存在する場合はFOLDER_HAS_CHILDREN例外がスローされる`() = runBlocking {
        val userId = UUID.randomUUID()
        val folderId = UUID.randomUUID()
        val input = DeleteFolderInput(userId = userId, folderId = folderId)
        coEvery { deleteFolderUseCase.execute(input) } throws
            DomainException(ErrorCode.FOLDER_HAS_CHILDREN)

        val exception = assertThrows<DomainException> {
            controller.deleteFolder(userId, folderId)
        }

        assertEquals(ErrorCode.FOLDER_HAS_CHILDREN, exception.code)
    }

    @Test
    fun `deleteFolder メモが存在する場合はFOLDER_HAS_MEMOS例外がスローされる`() = runBlocking {
        val userId = UUID.randomUUID()
        val folderId = UUID.randomUUID()
        val input = DeleteFolderInput(userId = userId, folderId = folderId)
        coEvery { deleteFolderUseCase.execute(input) } throws
            DomainException(ErrorCode.FOLDER_HAS_MEMOS)

        val exception = assertThrows<DomainException> {
            controller.deleteFolder(userId, folderId)
        }

        assertEquals(ErrorCode.FOLDER_HAS_MEMOS, exception.code)
    }
}

