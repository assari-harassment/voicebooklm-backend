package com.assari.voicebooklm.usecase.memo

import com.assari.voicebooklm.domain.model.Folder
import com.assari.voicebooklm.domain.model.MemoSortField
import com.assari.voicebooklm.domain.model.SortOrder
import com.assari.voicebooklm.domain.model.VoiceMemo
import com.assari.voicebooklm.domain.repository.FolderRepository
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
        val baseTime = Instant.parse("2024-01-01T00:00:00Z")
        val memo1 = VoiceMemo.create(
            id = UUID.randomUUID(),
            userId = userId,
            createdAt = baseTime,
            updatedAt = baseTime,
        )
        val memo2 = VoiceMemo.create(
            id = UUID.randomUUID(),
            userId = userId,
            createdAt = baseTime.plusSeconds(10),
            updatedAt = baseTime.plusSeconds(10),
        )
        val memo3 = VoiceMemo.create(
            id = UUID.randomUUID(),
            userId = userId,
            createdAt = baseTime.plusSeconds(20),
            updatedAt = baseTime.plusSeconds(20),
        )

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
        val baseTime = Instant.parse("2024-01-01T00:00:00Z")
        val memo1 = VoiceMemo.create(
            id = UUID.randomUUID(),
            userId = userId,
            createdAt = baseTime,
            updatedAt = baseTime,
        )
        val memo2 = VoiceMemo.create(
            id = UUID.randomUUID(),
            userId = userId,
            createdAt = baseTime.plusSeconds(10),
            updatedAt = baseTime.plusSeconds(10),
        )
        val memo3 = VoiceMemo.create(
            id = UUID.randomUUID(),
            userId = userId,
            createdAt = baseTime.plusSeconds(20),
            updatedAt = baseTime.plusSeconds(20),
        )

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
        assertEquals(3, result.total)
        assertTrue(result.hasMore)
    }

    @Test
    fun `offsetを指定すると先頭からスキップされる`() = runTest {
        val userId = UUID.randomUUID()
        val baseTime = Instant.parse("2024-01-01T00:00:00Z")
        val memo1 = VoiceMemo.create(
            id = UUID.randomUUID(),
            userId = userId,
            createdAt = baseTime,
            updatedAt = baseTime,
        )
        val memo2 = VoiceMemo.create(
            id = UUID.randomUUID(),
            userId = userId,
            createdAt = baseTime.plusSeconds(10),
            updatedAt = baseTime.plusSeconds(10),
        )
        val memo3 = VoiceMemo.create(
            id = UUID.randomUUID(),
            userId = userId,
            createdAt = baseTime.plusSeconds(20),
            updatedAt = baseTime.plusSeconds(20),
        )

        val voiceMemoRepository = InMemoryVoiceMemoRepository(
            initialMemos = listOf(memo1, memo2, memo3),
        )
        val folderRepository = InMemoryFolderRepository()
        val useCase = ListMemosUseCase(voiceMemoRepository, folderRepository)

        // 降順ソート（memo3, memo2, memo1の順）でoffset=1を指定
        val result = useCase.execute(
            ListMemosInput(
                userId = userId,
                sortBy = MemoSortField.UPDATED_AT,
                sortOrder = SortOrder.DESC,
                offset = 1,
            )
        )

        // memo3がスキップされ、memo2, memo1が返る
        assertEquals(2, result.memos.size)
        assertEquals(memo2.id, result.memos[0].memo.id)
        assertEquals(memo1.id, result.memos[1].memo.id)
        assertEquals(3, result.total)
        assertFalse(result.hasMore)
    }

    @Test
    fun `offset と limit を組み合わせてページネーションできる`() = runTest {
        val userId = UUID.randomUUID()
        val baseTime = Instant.parse("2024-01-01T00:00:00Z")
        val memos = (1..5).map { i ->
            VoiceMemo.create(
                id = UUID.randomUUID(),
                userId = userId,
                createdAt = baseTime.plusSeconds(i.toLong() * 10),
                updatedAt = baseTime.plusSeconds(i.toLong() * 10),
            )
        }

        val voiceMemoRepository = InMemoryVoiceMemoRepository(initialMemos = memos)
        val folderRepository = InMemoryFolderRepository()
        val useCase = ListMemosUseCase(voiceMemoRepository, folderRepository)

        // 1ページ目: offset=0, limit=2
        val page1 = useCase.execute(
            ListMemosInput(
                userId = userId,
                sortBy = MemoSortField.UPDATED_AT,
                sortOrder = SortOrder.DESC,
                offset = 0,
                limit = 2,
            )
        )
        assertEquals(2, page1.memos.size)
        assertEquals(5, page1.total)
        assertTrue(page1.hasMore)

        // 2ページ目: offset=2, limit=2
        val page2 = useCase.execute(
            ListMemosInput(
                userId = userId,
                sortBy = MemoSortField.UPDATED_AT,
                sortOrder = SortOrder.DESC,
                offset = 2,
                limit = 2,
            )
        )
        assertEquals(2, page2.memos.size)
        assertEquals(5, page2.total)
        assertTrue(page2.hasMore)

        // 3ページ目: offset=4, limit=2
        val page3 = useCase.execute(
            ListMemosInput(
                userId = userId,
                sortBy = MemoSortField.UPDATED_AT,
                sortOrder = SortOrder.DESC,
                offset = 4,
                limit = 2,
            )
        )
        assertEquals(1, page3.memos.size)
        assertEquals(5, page3.total)
        assertFalse(page3.hasMore)
    }

    @Test
    fun `全件取得時はhasMoreがfalseになる`() = runTest {
        val userId = UUID.randomUUID()
        val memo1 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
        val memo2 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)

        val voiceMemoRepository = InMemoryVoiceMemoRepository(
            initialMemos = listOf(memo1, memo2),
        )
        val folderRepository = InMemoryFolderRepository()
        val useCase = ListMemosUseCase(voiceMemoRepository, folderRepository)

        val result = useCase.execute(ListMemosInput(userId = userId))

        assertEquals(2, result.memos.size)
        assertEquals(2, result.total)
        assertFalse(result.hasMore)
    }

    @Test
    fun `メモが0件の場合はtotalが0でhasMoreがfalse`() = runTest {
        val folderRepository = InMemoryFolderRepository()
        val useCase = ListMemosUseCase(InMemoryVoiceMemoRepository(), folderRepository)

        val result = useCase.execute(ListMemosInput(userId = UUID.randomUUID()))

        assertTrue(result.memos.isEmpty())
        assertEquals(0, result.total)
        assertFalse(result.hasMore)
    }

    @Test
    fun `offsetが全件数以上の場合は空リストが返りhasMoreはfalse`() = runTest {
        val userId = UUID.randomUUID()
        val memo1 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
        val memo2 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)

        val voiceMemoRepository = InMemoryVoiceMemoRepository(
            initialMemos = listOf(memo1, memo2),
        )
        val folderRepository = InMemoryFolderRepository()
        val useCase = ListMemosUseCase(voiceMemoRepository, folderRepository)

        val result = useCase.execute(
            ListMemosInput(
                userId = userId,
                offset = 10,
            )
        )

        assertTrue(result.memos.isEmpty())
        assertEquals(2, result.total)
        assertFalse(result.hasMore)
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


    @Test
    fun `タイトルでキーワード検索できる`() = runTest {
        val userId = UUID.randomUUID()
        val memo1 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
            .completeTranscription("text1")
            .completeFormatting(title = "Kotlin開発のメモ", content = "content1", tags = emptyList())
        val memo2 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
            .completeTranscription("text2")
            .completeFormatting(title = "Java開発のメモ", content = "content2", tags = emptyList())
        val memo3 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
            .completeTranscription("text3")
            .completeFormatting(title = "会議メモ", content = "content3", tags = emptyList())

        val voiceMemoRepository = InMemoryVoiceMemoRepository(
            initialMemos = listOf(memo1, memo2, memo3),
        )
        val folderRepository = InMemoryFolderRepository()
        val useCase = ListMemosUseCase(voiceMemoRepository, folderRepository)

        val result = useCase.execute(
            ListMemosInput(
                userId = userId,
                keyword = "Kotlin",
            )
        )

        assertEquals(1, result.memos.size)
        assertEquals(memo1.id, result.memos[0].memo.id)
    }

    @Test
    fun `コンテントでキーワード検索できる`() = runTest {
        val userId = UUID.randomUUID()
        val memo1 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
            .completeTranscription("text1")
            .completeFormatting(title = "title1", content = "Springフレームワークを使った開発", tags = emptyList())
        val memo2 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
            .completeTranscription("text2")
            .completeFormatting(title = "title2", content = "Reactを使ったフロントエンド", tags = emptyList())
        val memo3 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
            .completeTranscription("text3")
            .completeFormatting(title = "title3", content = "データベース設計", tags = emptyList())

        val voiceMemoRepository = InMemoryVoiceMemoRepository(
            initialMemos = listOf(memo1, memo2, memo3),
        )
        val folderRepository = InMemoryFolderRepository()
        val useCase = ListMemosUseCase(voiceMemoRepository, folderRepository)

        val result = useCase.execute(
            ListMemosInput(
                userId = userId,
                keyword = "Spring",
            )
        )

        assertEquals(1, result.memos.size)
        assertEquals(memo1.id, result.memos[0].memo.id)
    }

    @Test
    fun `大文字小文字を区別せずキーワード検索できる`() = runTest {
        val userId = UUID.randomUUID()
        val memo1 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
            .completeTranscription("text1")
            .completeFormatting(title = "KOTLIN開発", content = "content1", tags = emptyList())
        val memo2 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
            .completeTranscription("text2")
            .completeFormatting(title = "title2", content = "kotlin入門", tags = emptyList())

        val voiceMemoRepository = InMemoryVoiceMemoRepository(
            initialMemos = listOf(memo1, memo2),
        )
        val folderRepository = InMemoryFolderRepository()
        val useCase = ListMemosUseCase(voiceMemoRepository, folderRepository)

        val result = useCase.execute(
            ListMemosInput(
                userId = userId,
                keyword = "kotlin",
            )
        )

        // 大文字小文字を区別せずマッチするので2件取得
        assertEquals(2, result.memos.size)
        val memoIds = result.memos.map { it.memo.id }
        assertTrue(memoIds.contains(memo1.id))
        assertTrue(memoIds.contains(memo2.id))
    }

    @Test
    fun `キーワードに該当するメモがない場合は空の一覧を返す`() = runTest {
        val userId = UUID.randomUUID()
        val memo1 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
            .completeTranscription("text1")
            .completeFormatting(title = "title1", content = "content1", tags = emptyList())

        val voiceMemoRepository = InMemoryVoiceMemoRepository(
            initialMemos = listOf(memo1),
        )
        val folderRepository = InMemoryFolderRepository()
        val useCase = ListMemosUseCase(voiceMemoRepository, folderRepository)

        val result = useCase.execute(
            ListMemosInput(
                userId = userId,
                keyword = "存在しないキーワード",
            )
        )

        assertTrue(result.memos.isEmpty())
    }

    @Test
    fun `削除済みメモはキーワード検索結果に含まれない`() = runTest {
        val userId = UUID.randomUUID()
        val memo1 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
            .completeTranscription("text1")
            .completeFormatting(title = "Kotlin開発", content = "content1", tags = emptyList())
        val memo2 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
            .completeTranscription("text2")
            .completeFormatting(title = "Kotlin入門", content = "content2", tags = emptyList())
            .markAsDeleted()  // 削除済み

        val voiceMemoRepository = InMemoryVoiceMemoRepository(
            initialMemos = listOf(memo1, memo2),
        )
        val folderRepository = InMemoryFolderRepository()
        val useCase = ListMemosUseCase(voiceMemoRepository, folderRepository)

        val result = useCase.execute(
            ListMemosInput(
                userId = userId,
                keyword = "Kotlin",
            )
        )

        // 削除済みメモは結果に含まれない
        assertEquals(1, result.memos.size)
        assertEquals(memo1.id, result.memos[0].memo.id)
    }

    @Test
    fun `単一タグでフィルタリングできる`() = runTest {
        val userId = UUID.randomUUID()
        val memo1 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
            .completeTranscription("text1")
            .completeFormatting(title = "title1", content = "content1", tags = listOf("開発", "Kotlin"))
        val memo2 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
            .completeTranscription("text2")
            .completeFormatting(title = "title2", content = "content2", tags = listOf("開発", "Java"))
        val memo3 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
            .completeTranscription("text3")
            .completeFormatting(title = "title3", content = "content3", tags = listOf("ミーティング"))

        val voiceMemoRepository = InMemoryVoiceMemoRepository(
            initialMemos = listOf(memo1, memo2, memo3),
        )
        val folderRepository = InMemoryFolderRepository()
        val useCase = ListMemosUseCase(voiceMemoRepository, folderRepository)

        val result = useCase.execute(
            ListMemosInput(
                userId = userId,
                tags = listOf("開発"),
            )
        )

        assertEquals(2, result.memos.size)
        val memoIds = result.memos.map { it.memo.id }
        assertTrue(memoIds.contains(memo1.id))
        assertTrue(memoIds.contains(memo2.id))
    }

    @Test
    fun `複数タグでAND検索できる`() = runTest {
        val userId = UUID.randomUUID()
        val memo1 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
            .completeTranscription("text1")
            .completeFormatting(title = "title1", content = "content1", tags = listOf("開発", "Kotlin"))
        val memo2 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
            .completeTranscription("text2")
            .completeFormatting(title = "title2", content = "content2", tags = listOf("開発", "Java"))
        val memo3 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
            .completeTranscription("text3")
            .completeFormatting(title = "title3", content = "content3", tags = listOf("開発", "Kotlin", "重要"))

        val voiceMemoRepository = InMemoryVoiceMemoRepository(
            initialMemos = listOf(memo1, memo2, memo3),
        )
        val folderRepository = InMemoryFolderRepository()
        val useCase = ListMemosUseCase(voiceMemoRepository, folderRepository)

        // 「開発」と「Kotlin」の両方を持つメモのみ取得
        val result = useCase.execute(
            ListMemosInput(
                userId = userId,
                tags = listOf("開発", "Kotlin"),
            )
        )

        assertEquals(2, result.memos.size)
        val memoIds = result.memos.map { it.memo.id }
        assertTrue(memoIds.contains(memo1.id))
        assertTrue(memoIds.contains(memo3.id))
    }

    @Test
    fun `タグ検索とキーワード検索を組み合わせできる`() = runTest {
        val userId = UUID.randomUUID()
        val memo1 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
            .completeTranscription("text1")
            .completeFormatting(title = "Kotlin開発入門", content = "content1", tags = listOf("開発"))
        val memo2 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
            .completeTranscription("text2")
            .completeFormatting(title = "Java開発入門", content = "content2", tags = listOf("開発"))
        val memo3 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
            .completeTranscription("text3")
            .completeFormatting(title = "Kotlinの基礎", content = "content3", tags = listOf("学習"))

        val voiceMemoRepository = InMemoryVoiceMemoRepository(
            initialMemos = listOf(memo1, memo2, memo3),
        )
        val folderRepository = InMemoryFolderRepository()
        val useCase = ListMemosUseCase(voiceMemoRepository, folderRepository)

        // 「開発」タグを持ち、かつタイトルに「Kotlin」を含むメモのみ取得
        val result = useCase.execute(
            ListMemosInput(
                userId = userId,
                keyword = "Kotlin",
                tags = listOf("開発"),
            )
        )

        assertEquals(1, result.memos.size)
        assertEquals(memo1.id, result.memos[0].memo.id)
    }

    @Test
    fun `タグ検索で大文字小文字を区別しない`() = runTest {
        val userId = UUID.randomUUID()
        val memo1 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
            .completeTranscription("text1")
            .completeFormatting(title = "title1", content = "content1", tags = listOf("Development"))
        val memo2 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
            .completeTranscription("text2")
            .completeFormatting(title = "title2", content = "content2", tags = listOf("DEVELOPMENT"))

        val voiceMemoRepository = InMemoryVoiceMemoRepository(
            initialMemos = listOf(memo1, memo2),
        )
        val folderRepository = InMemoryFolderRepository()
        val useCase = ListMemosUseCase(voiceMemoRepository, folderRepository)

        // 小文字で検索しても大文字・小文字のタグがマッチする
        val result = useCase.execute(
            ListMemosInput(
                userId = userId,
                tags = listOf("development"),
            )
        )

        assertEquals(2, result.memos.size)
    }

    @Test
    fun `該当するタグがない場合は空の一覧を返す`() = runTest {
        val userId = UUID.randomUUID()
        val memo1 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
            .completeTranscription("text1")
            .completeFormatting(title = "title1", content = "content1", tags = listOf("開発"))

        val voiceMemoRepository = InMemoryVoiceMemoRepository(
            initialMemos = listOf(memo1),
        )
        val folderRepository = InMemoryFolderRepository()
        val useCase = ListMemosUseCase(voiceMemoRepository, folderRepository)

        val result = useCase.execute(
            ListMemosInput(
                userId = userId,
                tags = listOf("存在しないタグ"),
            )
        )

        assertTrue(result.memos.isEmpty())
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
}
