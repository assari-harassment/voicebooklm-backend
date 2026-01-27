package com.assari.voicebooklm.usecase.folder

import com.assari.voicebooklm.domain.model.Folder
import com.assari.voicebooklm.domain.model.Formatting
import com.assari.voicebooklm.domain.model.Transcription
import com.assari.voicebooklm.domain.model.VoiceMemo
import com.assari.voicebooklm.usecase.memo.InMemoryVoiceMemoRepository
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
        val voiceMemoRepository = InMemoryVoiceMemoRepository()
        val useCase = ListFoldersUseCase(folderRepository, voiceMemoRepository)

        val result = useCase.execute(ListFoldersInput(userId = userId))

        assertEquals(2, result.folders.size)
        // メモがない場合は0が返される
        assertEquals(0, result.folders[0].memoCount)
        assertEquals(0, result.folders[1].memoCount)
    }

    @Test
    fun `フォルダーが0件の場合は空の一覧を返す`() = runTest {
        val folderRepository = InMemoryFolderRepository()
        val voiceMemoRepository = InMemoryVoiceMemoRepository()
        val useCase = ListFoldersUseCase(folderRepository, voiceMemoRepository)

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
        val voiceMemoRepository = InMemoryVoiceMemoRepository()
        val useCase = ListFoldersUseCase(folderRepository, voiceMemoRepository)

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
        val voiceMemoRepository = InMemoryVoiceMemoRepository()
        val useCase = ListFoldersUseCase(folderRepository, voiceMemoRepository)

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
        val voiceMemoRepository = InMemoryVoiceMemoRepository()
        val useCase = ListFoldersUseCase(folderRepository, voiceMemoRepository)

        val result = useCase.execute(ListFoldersInput(userId = userId))

        assertEquals(4, result.folders.size)
        val paths = result.folders.map { it.path }
        // パスでソートされる
        assertTrue(paths.contains("仕事"))
        assertTrue(paths.contains("仕事/会議"))
        assertTrue(paths.contains("プライベート"))
        assertTrue(paths.contains("プライベート/旅行"))
    }

    @Test
    fun `フォルダーにメモがある場合、メモ数が正しくカウントされる`() = runTest {
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
        val memo1 = VoiceMemo.restore(
            id = UUID.randomUUID(),
            userId = userId,
            transcription = Transcription.completed("テキスト1"),
            formatting = Formatting.completed("タイトル1", "コンテンツ1", folderId = folder1.id),
            deleted = false,
            createdAt = java.time.Instant.now(),
            updatedAt = java.time.Instant.now(),
        )
        val memo2 = VoiceMemo.restore(
            id = UUID.randomUUID(),
            userId = userId,
            transcription = Transcription.completed("テキスト2"),
            formatting = Formatting.completed("タイトル2", "コンテンツ2", folderId = folder1.id),
            deleted = false,
            createdAt = java.time.Instant.now(),
            updatedAt = java.time.Instant.now(),
        )
        val memo3 = VoiceMemo.restore(
            id = UUID.randomUUID(),
            userId = userId,
            transcription = Transcription.completed("テキスト3"),
            formatting = Formatting.completed("タイトル3", "コンテンツ3", folderId = folder2.id),
            deleted = false,
            createdAt = java.time.Instant.now(),
            updatedAt = java.time.Instant.now(),
        )
        val folderRepository = InMemoryFolderRepository(
            initialFolders = listOf(folder1, folder2)
        )
        val voiceMemoRepository = InMemoryVoiceMemoRepository(
            initialMemos = listOf(memo1, memo2, memo3)
        )
        val useCase = ListFoldersUseCase(folderRepository, voiceMemoRepository)

        val result = useCase.execute(ListFoldersInput(userId = userId))

        assertEquals(2, result.folders.size)
        val folder1Result = result.folders.find { it.folder.id == folder1.id }
        val folder2Result = result.folders.find { it.folder.id == folder2.id }
        requireNotNull(folder1Result)
        requireNotNull(folder2Result)
        assertEquals(2, folder1Result.memoCount)
        assertEquals(1, folder2Result.memoCount)
    }

    @Test
    fun `子フォルダー内のメモも親フォルダーのメモ数に含まれる`() = runTest {
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
        val parentMemo = VoiceMemo.restore(
            id = UUID.randomUUID(),
            userId = userId,
            transcription = Transcription.completed("テキスト1"),
            formatting = Formatting.completed("タイトル1", "コンテンツ1", folderId = parentFolder.id),
            deleted = false,
            createdAt = java.time.Instant.now(),
            updatedAt = java.time.Instant.now(),
        )
        val childMemo1 = VoiceMemo.restore(
            id = UUID.randomUUID(),
            userId = userId,
            transcription = Transcription.completed("テキスト2"),
            formatting = Formatting.completed("タイトル2", "コンテンツ2", folderId = childFolder.id),
            deleted = false,
            createdAt = java.time.Instant.now(),
            updatedAt = java.time.Instant.now(),
        )
        val childMemo2 = VoiceMemo.restore(
            id = UUID.randomUUID(),
            userId = userId,
            transcription = Transcription.completed("テキスト3"),
            formatting = Formatting.completed("タイトル3", "コンテンツ3", folderId = childFolder.id),
            deleted = false,
            createdAt = java.time.Instant.now(),
            updatedAt = java.time.Instant.now(),
        )
        val folderRepository = InMemoryFolderRepository(
            initialFolders = listOf(parentFolder, childFolder)
        )
        val voiceMemoRepository = InMemoryVoiceMemoRepository(
            initialMemos = listOf(parentMemo, childMemo1, childMemo2)
        )
        val useCase = ListFoldersUseCase(folderRepository, voiceMemoRepository)

        val result = useCase.execute(ListFoldersInput(userId = userId))

        assertEquals(2, result.folders.size)
        val parentResult = result.folders.find { it.folder.id == parentFolder.id }
        val childResult = result.folders.find { it.folder.id == childFolder.id }
        requireNotNull(parentResult)
        requireNotNull(childResult)
        // 親フォルダーには自分のメモ1件 + 子フォルダーのメモ2件 = 3件
        assertEquals(3, parentResult.memoCount)
        // 子フォルダーには自分のメモ2件
        assertEquals(2, childResult.memoCount)
    }

    @Test
    fun `削除済みメモはカウントに含まれない`() = runTest {
        val userId = UUID.randomUUID()
        val folder = Folder.create(
            id = UUID.randomUUID(),
            userId = userId,
            name = "仕事",
            parentId = null,
        )
        val activeMemo = VoiceMemo.restore(
            id = UUID.randomUUID(),
            userId = userId,
            transcription = Transcription.completed("テキスト1"),
            formatting = Formatting.completed("タイトル1", "コンテンツ1", folderId = folder.id),
            deleted = false,
            createdAt = java.time.Instant.now(),
            updatedAt = java.time.Instant.now(),
        )
        val deletedMemo = VoiceMemo.restore(
            id = UUID.randomUUID(),
            userId = userId,
            transcription = Transcription.completed("テキスト2"),
            formatting = Formatting.completed("タイトル2", "コンテンツ2", folderId = folder.id),
            deleted = true, // 削除済み
            createdAt = java.time.Instant.now(),
            updatedAt = java.time.Instant.now(),
        )
        val folderRepository = InMemoryFolderRepository(
            initialFolders = listOf(folder)
        )
        val voiceMemoRepository = InMemoryVoiceMemoRepository(
            initialMemos = listOf(activeMemo, deletedMemo)
        )
        val useCase = ListFoldersUseCase(folderRepository, voiceMemoRepository)

        val result = useCase.execute(ListFoldersInput(userId = userId))

        assertEquals(1, result.folders.size)
        // 削除済みメモはカウントに含まれない
        assertEquals(1, result.folders[0].memoCount)
    }

    @Test
    fun `他ユーザーのメモはカウントに含まれない`() = runTest {
        val userId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        val folder = Folder.create(
            id = UUID.randomUUID(),
            userId = userId,
            name = "仕事",
            parentId = null,
        )
        val userMemo = VoiceMemo.restore(
            id = UUID.randomUUID(),
            userId = userId,
            transcription = Transcription.completed("テキスト1"),
            formatting = Formatting.completed("タイトル1", "コンテンツ1", folderId = folder.id),
            deleted = false,
            createdAt = java.time.Instant.now(),
            updatedAt = java.time.Instant.now(),
        )
        val otherUserMemo = VoiceMemo.restore(
            id = UUID.randomUUID(),
            userId = otherUserId, // 他ユーザー
            transcription = Transcription.completed("テキスト2"),
            formatting = Formatting.completed("タイトル2", "コンテンツ2", folderId = folder.id),
            deleted = false,
            createdAt = java.time.Instant.now(),
            updatedAt = java.time.Instant.now(),
        )
        val folderRepository = InMemoryFolderRepository(
            initialFolders = listOf(folder)
        )
        val voiceMemoRepository = InMemoryVoiceMemoRepository(
            initialMemos = listOf(userMemo, otherUserMemo)
        )
        val useCase = ListFoldersUseCase(folderRepository, voiceMemoRepository)

        val result = useCase.execute(ListFoldersInput(userId = userId))

        assertEquals(1, result.folders.size)
        // 他ユーザーのメモはカウントに含まれない
        assertEquals(1, result.folders[0].memoCount)
    }
}
