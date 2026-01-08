package com.assari.voicebooklm.usecase.memo

import com.assari.voicebooklm.domain.model.Folder
import com.assari.voicebooklm.domain.model.VoiceMemo
import com.assari.voicebooklm.domain.repository.FolderRepository
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ListMemosUseCase の振る舞いをテストダブルで検証。
 */
class ListMemosUseCaseTest {

    @Test
    fun `ユーザーのメモ一覧を取得できる`() = runTest {
        val userId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        val memo1 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
        val memo2 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
        val otherMemo = VoiceMemo.create(id = UUID.randomUUID(), userId = otherUserId)

        val voiceMemoRepository = InMemoryVoiceMemoRepository(
            initialMemos = listOf(memo1, memo2, otherMemo),
        )
        val folderRepository = InMemoryFolderRepository()
        val useCase = ListMemosUseCase(voiceMemoRepository, folderRepository)

        val result = useCase.execute(ListMemosInput(userId = userId))

        // ユーザーのメモが2件取得できることを確認（順序はデフォルトソートに依存）
        assertEquals(2, result.memos.size)
        val memoIds = result.memos.map { it.memo.id }
        assertTrue(memoIds.contains(memo1.id))
        assertTrue(memoIds.contains(memo2.id))
    }

    @Test
    fun `メモが0件の場合は空の一覧を返す`() = runTest {
        val folderRepository = InMemoryFolderRepository()
        val useCase = ListMemosUseCase(InMemoryVoiceMemoRepository(), folderRepository)

        val result = useCase.execute(ListMemosInput(userId = UUID.randomUUID()))

        assertTrue(result.memos.isEmpty())
    }

    @Test
    fun `更新日時の降順でソートできる`() = runTest {
        val userId = UUID.randomUUID()
        val memo1 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
        Thread.sleep(10)
        val memo2 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
        Thread.sleep(10)
        val memo3 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)

        val voiceMemoRepository = InMemoryVoiceMemoRepository(
            initialMemos = listOf(memo1, memo2, memo3),
        )
        val folderRepository = InMemoryFolderRepository()
        val useCase = ListMemosUseCase(voiceMemoRepository, folderRepository)

        val result = useCase.execute(
            ListMemosInput(
                userId = userId,
                sortBy = MemoSortField.UPDATED_AT,
                sortOrder = SortOrder.DESC,
            )
        )

        assertEquals(3, result.memos.size)
        assertEquals(memo3.id, result.memos[0].memo.id)
        assertEquals(memo2.id, result.memos[1].memo.id)
        assertEquals(memo1.id, result.memos[2].memo.id)
    }

    @Test
    fun `更新日時の昇順でソートできる`() = runTest {
        val userId = UUID.randomUUID()
        val memo1 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
        Thread.sleep(10)
        val memo2 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
        Thread.sleep(10)
        val memo3 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)

        val voiceMemoRepository = InMemoryVoiceMemoRepository(
            initialMemos = listOf(memo1, memo2, memo3),
        )
        val folderRepository = InMemoryFolderRepository()
        val useCase = ListMemosUseCase(voiceMemoRepository, folderRepository)

        val result = useCase.execute(
            ListMemosInput(
                userId = userId,
                sortBy = MemoSortField.UPDATED_AT,
                sortOrder = SortOrder.ASC,
            )
        )

        assertEquals(3, result.memos.size)
        assertEquals(memo1.id, result.memos[0].memo.id)
        assertEquals(memo2.id, result.memos[1].memo.id)
        assertEquals(memo3.id, result.memos[2].memo.id)
    }

    @Test
    fun `件数制限が適用される`() = runTest {
        val userId = UUID.randomUUID()
        val memo1 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
        val memo2 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
        val memo3 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)

        val voiceMemoRepository = InMemoryVoiceMemoRepository(
            initialMemos = listOf(memo1, memo2, memo3),
        )
        val folderRepository = InMemoryFolderRepository()
        val useCase = ListMemosUseCase(voiceMemoRepository, folderRepository)

        val result = useCase.execute(
            ListMemosInput(
                userId = userId,
                limit = 2,
            )
        )

        assertEquals(2, result.memos.size)
    }

    @Test
    fun `タイトルの昇順でソートできる`() = runTest {
        val userId = UUID.randomUUID()
        val memo1 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
            .completeTranscription("text1")
            .completeFormatting(title = "C-Title", content = "content1", tags = emptyList())
        val memo2 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
            .completeTranscription("text2")
            .completeFormatting(title = "A-Title", content = "content2", tags = emptyList())
        val memo3 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
            .completeTranscription("text3")
            .completeFormatting(title = "B-Title", content = "content3", tags = emptyList())

        val voiceMemoRepository = InMemoryVoiceMemoRepository(
            initialMemos = listOf(memo1, memo2, memo3),
        )
        val folderRepository = InMemoryFolderRepository()
        val useCase = ListMemosUseCase(voiceMemoRepository, folderRepository)

        val result = useCase.execute(
            ListMemosInput(
                userId = userId,
                sortBy = MemoSortField.TITLE,
                sortOrder = SortOrder.ASC,
            )
        )

        assertEquals(3, result.memos.size)
        assertEquals("A-Title", result.memos[0].memo.formatting.title)
        assertEquals("B-Title", result.memos[1].memo.formatting.title)
        assertEquals("C-Title", result.memos[2].memo.formatting.title)
    }
}

// インメモリで動作する FolderRepository のテストダブル。
private class InMemoryFolderRepository(
    initialFolders: List<Folder> = emptyList(),
) : FolderRepository {
    private val folders = initialFolders.toMutableList()

    override suspend fun save(folder: Folder): Folder {
        folders.removeIf { it.id == folder.id }
        folders += folder
        return folder
    }

    override suspend fun findById(id: UUID): Folder? = folders.find { it.id == id }

    override suspend fun findByUserId(userId: UUID): List<Folder> = folders.filter { it.userId == userId }

    override suspend fun findByUserIdAndParentId(userId: UUID, parentId: UUID?): List<Folder> =
        folders.filter { it.userId == userId && it.parentId == parentId }

    override suspend fun findByUserIdAndPath(userId: UUID, path: String): Folder? = null

    override suspend fun findDescendantIds(folderId: UUID): List<UUID> = emptyList()

    override suspend fun delete(id: UUID) {
        folders.removeIf { it.id == id }
    }

    override suspend fun existsByUserIdAndParentIdAndName(
        userId: UUID,
        parentId: UUID?,
        name: String,
        excludeId: UUID?,
    ): Boolean =
        folders.any {
            it.userId == userId &&
                it.parentId == parentId &&
                it.name == name &&
                (excludeId == null || it.id != excludeId)
        }

    override fun deleteByUserId(userId: UUID) {
        folders.removeIf { it.userId == userId }
    }
}
