package com.assari.voicebooklm.usecase.memo

import com.assari.voicebooklm.domain.exception.DomainException
import com.assari.voicebooklm.domain.exception.ErrorCode
import com.assari.voicebooklm.domain.model.Folder
import com.assari.voicebooklm.domain.model.VoiceMemo
import com.assari.voicebooklm.usecase.folder.InMemoryFolderRepository
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * UpdateMemoUseCase の振る舞いをテストダブルで検証。
 */
class UpdateMemoUseCaseTest {

    /**
     * 整形完了済みのメモを作成するヘルパー
     */
    private fun createCompletedMemo(
        id: UUID = UUID.randomUUID(),
        userId: UUID,
        title: String = "テストタイトル",
        content: String = "テスト本文",
        tags: List<String> = listOf("tag1", "tag2"),
        folderId: UUID? = null,
    ): VoiceMemo {
        return VoiceMemo.create(id = id, userId = userId)
            .startTranscription()
            .completeTranscription("テスト文字起こし")
            .startFormatting()
            .completeFormatting(
                title = title,
                content = content,
                tags = tags,
                folderId = folderId,
            )
    }

    @Test
    fun `タイトルのみ更新できる`() = runTest {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()
        val memo = createCompletedMemo(id = memoId, userId = userId, title = "旧タイトル")

        val voiceMemoRepository = InMemoryVoiceMemoRepository(initialMemos = listOf(memo))
        val folderRepository = InMemoryFolderRepository()
        val useCase = UpdateMemoUseCase(voiceMemoRepository, folderRepository)

        val result = useCase.execute(
            UpdateMemoInput(
                memoId = memoId,
                userId = userId,
                title = "新タイトル",
                content = null,
                tags = null,
                folderId = null,
                removeFolder = false,
            )
        )

        assertEquals("新タイトル", result.memo.title)
        assertEquals("テスト本文", result.memo.content) // 変更されていない
        assertEquals(listOf("tag1", "tag2"), result.memo.tags) // 変更されていない
    }

    @Test
    fun `本文のみ更新できる`() = runTest {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()
        val memo = createCompletedMemo(id = memoId, userId = userId, content = "旧本文")

        val voiceMemoRepository = InMemoryVoiceMemoRepository(initialMemos = listOf(memo))
        val folderRepository = InMemoryFolderRepository()
        val useCase = UpdateMemoUseCase(voiceMemoRepository, folderRepository)

        val result = useCase.execute(
            UpdateMemoInput(
                memoId = memoId,
                userId = userId,
                title = null,
                content = "新本文",
                tags = null,
                folderId = null,
                removeFolder = false,
            )
        )

        assertEquals("テストタイトル", result.memo.title) // 変更されていない
        assertEquals("新本文", result.memo.content)
        assertEquals(listOf("tag1", "tag2"), result.memo.tags) // 変更されていない
    }

    @Test
    fun `タグのみ更新できる`() = runTest {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()
        val memo = createCompletedMemo(id = memoId, userId = userId, tags = listOf("old1", "old2"))

        val voiceMemoRepository = InMemoryVoiceMemoRepository(initialMemos = listOf(memo))
        val folderRepository = InMemoryFolderRepository()
        val useCase = UpdateMemoUseCase(voiceMemoRepository, folderRepository)

        val result = useCase.execute(
            UpdateMemoInput(
                memoId = memoId,
                userId = userId,
                title = null,
                content = null,
                tags = listOf("new1", "new2", "new3"),
                folderId = null,
                removeFolder = false,
            )
        )

        assertEquals("テストタイトル", result.memo.title) // 変更されていない
        assertEquals("テスト本文", result.memo.content) // 変更されていない
        assertEquals(listOf("new1", "new2", "new3"), result.memo.tags)
    }

    @Test
    fun `フォルダーのみ更新できる`() = runTest {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()
        val folderId = UUID.randomUUID()
        val folder = Folder.create(id = folderId, userId = userId, name = "テストフォルダー")
        val memo = createCompletedMemo(id = memoId, userId = userId, folderId = null)

        val voiceMemoRepository = InMemoryVoiceMemoRepository(initialMemos = listOf(memo))
        val folderRepository = InMemoryFolderRepository(initialFolders = listOf(folder))
        val useCase = UpdateMemoUseCase(voiceMemoRepository, folderRepository)

        val result = useCase.execute(
            UpdateMemoInput(
                memoId = memoId,
                userId = userId,
                title = null,
                content = null,
                tags = null,
                folderId = folderId,
                removeFolder = false,
            )
        )

        assertEquals("テストタイトル", result.memo.title) // 変更されていない
        assertEquals("テスト本文", result.memo.content) // 変更されていない
        assertEquals(listOf("tag1", "tag2"), result.memo.tags) // 変更されていない
        assertEquals(folderId, result.memo.formatting.folderId)
    }

    @Test
    fun `複数フィールド同時更新できる`() = runTest {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()
        val memo = createCompletedMemo(
            id = memoId,
            userId = userId,
            title = "旧タイトル",
            content = "旧本文",
            tags = listOf("old"),
        )

        val voiceMemoRepository = InMemoryVoiceMemoRepository(initialMemos = listOf(memo))
        val folderRepository = InMemoryFolderRepository()
        val useCase = UpdateMemoUseCase(voiceMemoRepository, folderRepository)

        val result = useCase.execute(
            UpdateMemoInput(
                memoId = memoId,
                userId = userId,
                title = "新タイトル",
                content = "新本文",
                tags = listOf("new1", "new2"),
                folderId = null,
                removeFolder = false,
            )
        )

        assertEquals("新タイトル", result.memo.title)
        assertEquals("新本文", result.memo.content)
        assertEquals(listOf("new1", "new2"), result.memo.tags)
    }

    @Test
    fun `フォルダー解除できる`() = runTest {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()
        val folderId = UUID.randomUUID()
        val memo = createCompletedMemo(id = memoId, userId = userId, folderId = folderId)

        val voiceMemoRepository = InMemoryVoiceMemoRepository(initialMemos = listOf(memo))
        val folderRepository = InMemoryFolderRepository()
        val useCase = UpdateMemoUseCase(voiceMemoRepository, folderRepository)

        val result = useCase.execute(
            UpdateMemoInput(
                memoId = memoId,
                userId = userId,
                title = null,
                content = null,
                tags = null,
                folderId = null,
                removeFolder = true,
            )
        )

        assertNull(result.memo.formatting.folderId)
    }

    @Test
    fun `他人のメモを更新しようとするとMEMO_NOT_FOUNDエラーになる`() = runTest {
        val ownerId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        val memoId = UUID.randomUUID()
        val memo = createCompletedMemo(id = memoId, userId = ownerId)

        val voiceMemoRepository = InMemoryVoiceMemoRepository(initialMemos = listOf(memo))
        val folderRepository = InMemoryFolderRepository()
        val useCase = UpdateMemoUseCase(voiceMemoRepository, folderRepository)

        val exception = assertThrows<DomainException> {
            useCase.execute(
                UpdateMemoInput(
                    memoId = memoId,
                    userId = otherUserId,
                    title = "新タイトル",
                    content = null,
                    tags = null,
                    folderId = null,
                    removeFolder = false,
                )
            )
        }

        // メモの存在を推測されないよう、403ではなく404を返す
        assertEquals(ErrorCode.MEMO_NOT_FOUND, exception.code)
    }

    @Test
    fun `整形未完了のメモを更新しようとするとMEMO_NOT_COMPLETEDエラーになる`() = runTest {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()
        // 文字起こしのみ完了、整形は未完了
        val memo = VoiceMemo.create(id = memoId, userId = userId)
            .startTranscription()
            .completeTranscription("テスト文字起こし")

        val voiceMemoRepository = InMemoryVoiceMemoRepository(initialMemos = listOf(memo))
        val folderRepository = InMemoryFolderRepository()
        val useCase = UpdateMemoUseCase(voiceMemoRepository, folderRepository)

        val exception = assertThrows<DomainException> {
            useCase.execute(
                UpdateMemoInput(
                    memoId = memoId,
                    userId = userId,
                    title = "新タイトル",
                    content = null,
                    tags = null,
                    folderId = null,
                    removeFolder = false,
                )
            )
        }

        assertEquals(ErrorCode.MEMO_NOT_COMPLETED, exception.code)
    }

    @Test
    fun `存在しないメモを更新しようとするとMEMO_NOT_FOUNDエラーになる`() = runTest {
        val userId = UUID.randomUUID()
        val nonExistentMemoId = UUID.randomUUID()

        val voiceMemoRepository = InMemoryVoiceMemoRepository()
        val folderRepository = InMemoryFolderRepository()
        val useCase = UpdateMemoUseCase(voiceMemoRepository, folderRepository)

        val exception = assertThrows<DomainException> {
            useCase.execute(
                UpdateMemoInput(
                    memoId = nonExistentMemoId,
                    userId = userId,
                    title = "新タイトル",
                    content = null,
                    tags = null,
                    folderId = null,
                    removeFolder = false,
                )
            )
        }

        assertEquals(ErrorCode.MEMO_NOT_FOUND, exception.code)
    }

    @Test
    fun `存在しないフォルダーを指定するとFOLDER_NOT_FOUNDエラーになる`() = runTest {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()
        val nonExistentFolderId = UUID.randomUUID()
        val memo = createCompletedMemo(id = memoId, userId = userId)

        val voiceMemoRepository = InMemoryVoiceMemoRepository(initialMemos = listOf(memo))
        val folderRepository = InMemoryFolderRepository()
        val useCase = UpdateMemoUseCase(voiceMemoRepository, folderRepository)

        val exception = assertThrows<DomainException> {
            useCase.execute(
                UpdateMemoInput(
                    memoId = memoId,
                    userId = userId,
                    title = null,
                    content = null,
                    tags = null,
                    folderId = nonExistentFolderId,
                    removeFolder = false,
                )
            )
        }

        assertEquals(ErrorCode.FOLDER_NOT_FOUND, exception.code)
    }

    @Test
    fun `他人のフォルダーを指定するとFOLDER_NOT_FOUNDエラーになる`() = runTest {
        val userId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        val memoId = UUID.randomUUID()
        val folderId = UUID.randomUUID()
        val otherUserFolder = Folder.create(id = folderId, userId = otherUserId, name = "他人のフォルダー")
        val memo = createCompletedMemo(id = memoId, userId = userId)

        val voiceMemoRepository = InMemoryVoiceMemoRepository(initialMemos = listOf(memo))
        val folderRepository = InMemoryFolderRepository(initialFolders = listOf(otherUserFolder))
        val useCase = UpdateMemoUseCase(voiceMemoRepository, folderRepository)

        val exception = assertThrows<DomainException> {
            useCase.execute(
                UpdateMemoInput(
                    memoId = memoId,
                    userId = userId,
                    title = null,
                    content = null,
                    tags = null,
                    folderId = folderId,
                    removeFolder = false,
                )
            )
        }

        // フォルダーの存在を推測されないよう、403ではなく404を返す
        assertEquals(ErrorCode.FOLDER_NOT_FOUND, exception.code)
    }
}