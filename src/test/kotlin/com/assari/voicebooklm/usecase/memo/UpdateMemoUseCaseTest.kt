package com.assari.voicebooklm.usecase.memo

import com.assari.voicebooklm.domain.exception.DomainException
import com.assari.voicebooklm.domain.exception.ErrorCode
import com.assari.voicebooklm.domain.model.VoiceMemo
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
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
    ): VoiceMemo {
        return VoiceMemo.create(id = id, userId = userId)
            .startTranscription()
            .completeTranscription("テスト文字起こし")
            .startFormatting()
            .completeFormatting(
                title = title,
                content = content,
                tags = tags,
                folderId = null,
            )
    }

    @Test
    fun `タイトルのみ更新できる`() = runTest {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()
        val memo = createCompletedMemo(id = memoId, userId = userId, title = "旧タイトル")

        val voiceMemoRepository = InMemoryVoiceMemoRepository(initialMemos = listOf(memo))
        val useCase = UpdateMemoUseCase(voiceMemoRepository)

        val result = useCase.execute(
            UpdateMemoInput(
                memoId = memoId,
                userId = userId,
                title = "新タイトル",
                content = null,
                tags = null,
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
        val useCase = UpdateMemoUseCase(voiceMemoRepository)

        val result = useCase.execute(
            UpdateMemoInput(
                memoId = memoId,
                userId = userId,
                title = null,
                content = "新本文",
                tags = null,
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
        val useCase = UpdateMemoUseCase(voiceMemoRepository)

        val result = useCase.execute(
            UpdateMemoInput(
                memoId = memoId,
                userId = userId,
                title = null,
                content = null,
                tags = listOf("new1", "new2", "new3"),
            )
        )

        assertEquals("テストタイトル", result.memo.title) // 変更されていない
        assertEquals("テスト本文", result.memo.content) // 変更されていない
        assertEquals(listOf("new1", "new2", "new3"), result.memo.tags)
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
        val useCase = UpdateMemoUseCase(voiceMemoRepository)

        val result = useCase.execute(
            UpdateMemoInput(
                memoId = memoId,
                userId = userId,
                title = "新タイトル",
                content = "新本文",
                tags = listOf("new1", "new2"),
            )
        )

        assertEquals("新タイトル", result.memo.title)
        assertEquals("新本文", result.memo.content)
        assertEquals(listOf("new1", "new2"), result.memo.tags)
    }

    @Test
    fun `他人のメモを更新しようとするとMEMO_NOT_FOUNDエラーになる`() = runTest {
        val ownerId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        val memoId = UUID.randomUUID()
        val memo = createCompletedMemo(id = memoId, userId = ownerId)

        val voiceMemoRepository = InMemoryVoiceMemoRepository(initialMemos = listOf(memo))
        val useCase = UpdateMemoUseCase(voiceMemoRepository)

        val exception = assertThrows<DomainException> {
            useCase.execute(
                UpdateMemoInput(
                    memoId = memoId,
                    userId = otherUserId,
                    title = "新タイトル",
                    content = null,
                    tags = null,
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
        val useCase = UpdateMemoUseCase(voiceMemoRepository)

        val exception = assertThrows<DomainException> {
            useCase.execute(
                UpdateMemoInput(
                    memoId = memoId,
                    userId = userId,
                    title = "新タイトル",
                    content = null,
                    tags = null,
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
        val useCase = UpdateMemoUseCase(voiceMemoRepository)

        val exception = assertThrows<DomainException> {
            useCase.execute(
                UpdateMemoInput(
                    memoId = nonExistentMemoId,
                    userId = userId,
                    title = "新タイトル",
                    content = null,
                    tags = null,
                )
            )
        }

        assertEquals(ErrorCode.MEMO_NOT_FOUND, exception.code)
    }

    @Test
    fun `整形失敗のメモを更新しようとするとFORMATTING_FAILEDエラーになる`() = runTest {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()
        // 整形処理が失敗した状態
        val memo = VoiceMemo.create(id = memoId, userId = userId)
            .startTranscription()
            .completeTranscription("テスト文字起こし")
            .startFormatting()
            .failFormatting()

        val voiceMemoRepository = InMemoryVoiceMemoRepository(initialMemos = listOf(memo))
        val useCase = UpdateMemoUseCase(voiceMemoRepository)

        val exception = assertThrows<DomainException> {
            useCase.execute(
                UpdateMemoInput(
                    memoId = memoId,
                    userId = userId,
                    title = "新タイトル",
                    content = null,
                    tags = null,
                )
            )
        }

        assertEquals(ErrorCode.FORMATTING_FAILED, exception.code)
    }

    @Test
    fun `全フィールドnullで更新しても元の値が保持される`() = runTest {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()
        val memo = createCompletedMemo(
            id = memoId,
            userId = userId,
            title = "元のタイトル",
            content = "元の本文",
            tags = listOf("tag1", "tag2"),
        )

        val voiceMemoRepository = InMemoryVoiceMemoRepository(initialMemos = listOf(memo))
        val useCase = UpdateMemoUseCase(voiceMemoRepository)

        val result = useCase.execute(
            UpdateMemoInput(
                memoId = memoId,
                userId = userId,
                title = null,
                content = null,
                tags = null,
            )
        )

        assertEquals("元のタイトル", result.memo.title)
        assertEquals("元の本文", result.memo.content)
        assertEquals(listOf("tag1", "tag2"), result.memo.tags)
    }
}
