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
        val memo1 = VoiceMemo.create(userId = userId)
        val memo2 = VoiceMemo.create(userId = userId)
        val otherMemo = VoiceMemo.create(userId = otherUserId)

        val voiceMemoRepository = FakeVoiceMemoRepository(
            initialMemos = listOf(memo1, memo2, otherMemo),
        )
        val useCase = ListMemosUseCase(voiceMemoRepository)

        val result = useCase.execute(ListMemosInput(userId = userId))

        assertEquals(listOf(memo1, memo2), result.memos)
    }

    @Test
    fun `メモが0件の場合は空の一覧を返す`() = runTest {
        val useCase = ListMemosUseCase(FakeVoiceMemoRepository())

        val result = useCase.execute(ListMemosInput(userId = UUID.randomUUID()))

        assertEquals(emptyList<VoiceMemo>(), result.memos)
    }
}

// インメモリで動作する VoiceMemoRepository のテストダブル。
private class FakeVoiceMemoRepository(
    initialMemos: List<VoiceMemo> = emptyList(),
) : VoiceMemoRepository {
    private val memos = initialMemos.toMutableList()

    override suspend fun save(voiceMemo: VoiceMemo): VoiceMemo {
        memos += voiceMemo
        return voiceMemo
    }

    override suspend fun findById(id: UUID): VoiceMemo? = memos.find { it.id == id }

    override suspend fun findByUserId(userId: UUID): List<VoiceMemo> = memos.filter { it.userId == userId }

    override fun deleteByUserId(userId: UUID) {
        memos.removeIf { it.userId == userId }
    }
}
