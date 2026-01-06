package com.assari.voicebooklm.usecase.memo

import com.assari.voicebooklm.domain.exception.DomainException
import com.assari.voicebooklm.domain.exception.ErrorCode
import com.assari.voicebooklm.domain.model.VoiceMemo
import com.assari.voicebooklm.domain.repository.VoiceMemoRepository
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * DeleteMemoUseCase の振る舞いをテストダブルで検証。
 */
class DeleteMemoUseCaseTest {

    @Test
    fun `自分のメモを削除できる`() = runTest {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()
        val memo = VoiceMemo.create(id = memoId, userId = userId)

        val voiceMemoRepository = InMemoryVoiceMemoRepository(
            initialMemos = listOf(memo),
        )
        val useCase = DeleteMemoUseCase(voiceMemoRepository)

        useCase.execute(DeleteMemoInput(memoId = memoId, userId = userId))

        // 削除フラグが立っていることを確認
        val deletedMemo = voiceMemoRepository.findByIdIncludingDeleted(memoId)
        assertTrue(deletedMemo?.deleted == true)

        // findByIdでは削除済みメモは取得できないことを確認
        assertEquals(null, voiceMemoRepository.findById(memoId))
    }

    @Test
    fun `他人のメモを削除しようとするとUNAUTHORIZED_ACCESSエラーになる`() = runTest {
        val ownerId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        val memoId = UUID.randomUUID()
        val memo = VoiceMemo.create(id = memoId, userId = ownerId)

        val voiceMemoRepository = InMemoryVoiceMemoRepository(
            initialMemos = listOf(memo),
        )
        val useCase = DeleteMemoUseCase(voiceMemoRepository)

        val exception = assertThrows<DomainException> {
            useCase.execute(DeleteMemoInput(memoId = memoId, userId = otherUserId))
        }

        assertEquals(ErrorCode.UNAUTHORIZED_ACCESS, exception.code)

        // メモは削除されていないことを確認
        val unchangedMemo = voiceMemoRepository.findById(memoId)
        assertEquals(false, unchangedMemo?.deleted)
    }

    @Test
    fun `存在しないメモを削除しようとするとMEMO_NOT_FOUNDエラーになる`() = runTest {
        val userId = UUID.randomUUID()
        val nonExistentMemoId = UUID.randomUUID()

        val voiceMemoRepository = InMemoryVoiceMemoRepository()
        val useCase = DeleteMemoUseCase(voiceMemoRepository)

        val exception = assertThrows<DomainException> {
            useCase.execute(DeleteMemoInput(memoId = nonExistentMemoId, userId = userId))
        }

        assertEquals(ErrorCode.MEMO_NOT_FOUND, exception.code)
    }

    @Test
    fun `既に削除済みのメモを削除しようとするとMEMO_NOT_FOUNDエラーになる`() = runTest {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()
        val memo = VoiceMemo.create(id = memoId, userId = userId).markAsDeleted()

        val voiceMemoRepository = InMemoryVoiceMemoRepository(
            initialMemos = listOf(memo),
        )
        val useCase = DeleteMemoUseCase(voiceMemoRepository)

        val exception = assertThrows<DomainException> {
            useCase.execute(DeleteMemoInput(memoId = memoId, userId = userId))
        }

        assertEquals(ErrorCode.MEMO_NOT_FOUND, exception.code)
    }
}
