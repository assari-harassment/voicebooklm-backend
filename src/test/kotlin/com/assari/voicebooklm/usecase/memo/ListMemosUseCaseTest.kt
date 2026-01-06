package com.assari.voicebooklm.usecase.memo

import com.assari.voicebooklm.domain.model.VoiceMemo
import com.assari.voicebooklm.domain.repository.VoiceMemoRepository
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
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
        val useCase = ListMemosUseCase(voiceMemoRepository)

        val result = useCase.execute(ListMemosInput(userId = userId))

        assertEquals(listOf(memo1, memo2), result.memos)
    }

    @Test
    fun `メモが0件の場合は空の一覧を返す`() = runTest {
        val useCase = ListMemosUseCase(InMemoryVoiceMemoRepository())

        val result = useCase.execute(ListMemosInput(userId = UUID.randomUUID()))

        assertEquals(emptyList<VoiceMemo>(), result.memos)
    }
}
