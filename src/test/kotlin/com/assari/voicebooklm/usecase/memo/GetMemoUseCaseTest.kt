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
        val useCase = GetMemoUseCase(voiceMemoRepository)

        val result = useCase.execute(GetMemoInput(memoId = memoId, userId = userId))

        // 取得したメモが正しいことを確認
        assertEquals(memoId, result.memo.id)
        assertEquals(userId, result.memo.userId)
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
        val useCase = GetMemoUseCase(voiceMemoRepository)

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
        val useCase = GetMemoUseCase(voiceMemoRepository)

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
        val useCase = GetMemoUseCase(voiceMemoRepository)

        val exception = assertThrows<DomainException> {
            useCase.execute(GetMemoInput(memoId = memoId, userId = userId))
        }

        assertEquals(ErrorCode.MEMO_NOT_FOUND, exception.code)
    }
}
