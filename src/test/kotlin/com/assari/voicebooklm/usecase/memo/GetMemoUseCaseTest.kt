package com.assari.voicebooklm.usecase.memo

import com.assari.voicebooklm.domain.exception.DomainException
import com.assari.voicebooklm.domain.exception.ErrorCode
import com.assari.voicebooklm.domain.model.Folder
import com.assari.voicebooklm.domain.model.VoiceMemo
import com.assari.voicebooklm.usecase.folder.InMemoryFolderRepository
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * GetMemoUseCase の振る舞いをテストダブルで検証。
 */
class GetMemoUseCaseTest {

    @Test
    fun `自分のメモを取得できる`() = runTest {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()
        val memo = VoiceMemo.create(id = memoId, userId = userId)

        val voiceMemoRepository = InMemoryVoiceMemoRepository(
            initialMemos = listOf(memo),
        )
        val folderRepository = InMemoryFolderRepository()
        val useCase = GetMemoUseCase(voiceMemoRepository, folderRepository)

        val result = useCase.execute(GetMemoInput(memoId = memoId, userId = userId))

        // 取得したメモが正しいことを確認
        assertEquals(memoId, result.memo.id)
        assertEquals(userId, result.memo.userId)
        // フォルダー情報は null
        assertNull(result.folder)
        assertNull(result.folderPath)
    }

    @Test
    fun `他人のメモを取得しようとするとMEMO_NOT_FOUNDエラーになる`() = runTest {
        val ownerId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        val memoId = UUID.randomUUID()
        val memo = VoiceMemo.create(id = memoId, userId = ownerId)

        val voiceMemoRepository = InMemoryVoiceMemoRepository(
            initialMemos = listOf(memo),
        )
        val folderRepository = InMemoryFolderRepository()
        val useCase = GetMemoUseCase(voiceMemoRepository, folderRepository)

        val exception = assertThrows<DomainException> {
            useCase.execute(GetMemoInput(memoId = memoId, userId = otherUserId))
        }

        // メモの存在を推測されないよう、403ではなく404を返す
        assertEquals(ErrorCode.MEMO_NOT_FOUND, exception.code)
    }

    @Test
    fun `存在しないメモを取得しようとするとMEMO_NOT_FOUNDエラーになる`() = runTest {
        val userId = UUID.randomUUID()
        val nonExistentMemoId = UUID.randomUUID()

        val voiceMemoRepository = InMemoryVoiceMemoRepository()
        val folderRepository = InMemoryFolderRepository()
        val useCase = GetMemoUseCase(voiceMemoRepository, folderRepository)

        val exception = assertThrows<DomainException> {
            useCase.execute(GetMemoInput(memoId = nonExistentMemoId, userId = userId))
        }

        assertEquals(ErrorCode.MEMO_NOT_FOUND, exception.code)
    }

    @Test
    fun `削除済みのメモを取得しようとするとMEMO_NOT_FOUNDエラーになる`() = runTest {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()
        val memo = VoiceMemo.create(id = memoId, userId = userId).markAsDeleted()

        val voiceMemoRepository = InMemoryVoiceMemoRepository(
            initialMemos = listOf(memo),
        )
        val folderRepository = InMemoryFolderRepository()
        val useCase = GetMemoUseCase(voiceMemoRepository, folderRepository)

        val exception = assertThrows<DomainException> {
            useCase.execute(GetMemoInput(memoId = memoId, userId = userId))
        }

        assertEquals(ErrorCode.MEMO_NOT_FOUND, exception.code)
    }

    @Test
    fun `フォルダーに紐づくメモを取得するとフォルダー情報が含まれる`() = runTest {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()
        val folderId = UUID.randomUUID()
        val folder = Folder.create(
            id = folderId,
            userId = userId,
            name = "仕事",
        )

        // フォルダーに紐づくメモを作成
        val memo = VoiceMemo.create(id = memoId, userId = userId)
            .completeFormatting(
                title = "テストメモ",
                content = "コンテンツ",
                tags = emptyList(),
                folderId = folderId,
            )

        val voiceMemoRepository = InMemoryVoiceMemoRepository(
            initialMemos = listOf(memo),
        )
        val folderRepository = InMemoryFolderRepository(
            initialFolders = listOf(folder),
        )
        val useCase = GetMemoUseCase(voiceMemoRepository, folderRepository)

        val result = useCase.execute(GetMemoInput(memoId = memoId, userId = userId))

        // メモ情報を確認
        assertEquals(memoId, result.memo.id)
        assertEquals(userId, result.memo.userId)

        // フォルダー情報を確認
        assertNotNull(result.folder)
        assertEquals(folderId, result.folder?.id)
        assertEquals("仕事", result.folder?.name)
        assertEquals("仕事", result.folderPath)
    }

    @Test
    fun `階層フォルダーに紐づくメモを取得するとフルパスが含まれる`() = runTest {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()
        val parentFolderId = UUID.randomUUID()
        val childFolderId = UUID.randomUUID()

        val parentFolder = Folder.create(
            id = parentFolderId,
            userId = userId,
            name = "仕事",
        )
        val childFolder = Folder.create(
            id = childFolderId,
            userId = userId,
            name = "プロジェクトA",
            parentId = parentFolderId,
        )

        // 子フォルダーに紐づくメモを作成
        val memo = VoiceMemo.create(id = memoId, userId = userId)
            .completeFormatting(
                title = "テストメモ",
                content = "コンテンツ",
                tags = emptyList(),
                folderId = childFolderId,
            )

        val voiceMemoRepository = InMemoryVoiceMemoRepository(
            initialMemos = listOf(memo),
        )
        val folderRepository = InMemoryFolderRepository(
            initialFolders = listOf(parentFolder, childFolder),
        )
        val useCase = GetMemoUseCase(voiceMemoRepository, folderRepository)

        val result = useCase.execute(GetMemoInput(memoId = memoId, userId = userId))

        // メモ情報を確認
        assertEquals(memoId, result.memo.id)

        // フォルダー情報を確認
        assertNotNull(result.folder)
        assertEquals(childFolderId, result.folder?.id)
        assertEquals("プロジェクトA", result.folder?.name)
        assertEquals("仕事/プロジェクトA", result.folderPath)
    }

    @Test
    fun `削除されたフォルダーに紐づくメモを取得するとfolderとfolderPathがnullになる`() = runTest {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()
        val deletedFolderId = UUID.randomUUID()

        // フォルダーに紐づくメモを作成（フォルダーは既に削除されている）
        val memo = VoiceMemo.create(id = memoId, userId = userId)
            .completeFormatting(
                title = "テストメモ",
                content = "コンテンツ",
                tags = emptyList(),
                folderId = deletedFolderId,
            )

        val voiceMemoRepository = InMemoryVoiceMemoRepository(
            initialMemos = listOf(memo),
        )
        // フォルダーは存在しない（削除済み）ため、空のリポジトリを使用
        val folderRepository = InMemoryFolderRepository(
            initialFolders = emptyList(),
        )
        val useCase = GetMemoUseCase(voiceMemoRepository, folderRepository)

        val result = useCase.execute(GetMemoInput(memoId = memoId, userId = userId))

        // メモ情報を確認
        assertEquals(memoId, result.memo.id)
        assertEquals(userId, result.memo.userId)

        // フォルダー情報はnull（フォルダーが存在しないため）
        assertNull(result.folder)
        assertNull(result.folderPath)
    }
}
