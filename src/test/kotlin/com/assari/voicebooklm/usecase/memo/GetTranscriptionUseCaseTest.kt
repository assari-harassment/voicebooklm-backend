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
 * GetTranscriptionUseCase の振る舞いをテストダブルで検証。
 */
class GetTranscriptionUseCaseTest {

    @Test
    fun `文字起こしが完了しているメモから文字起こしテキストを取得できる`() = runTest {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()
        val transcriptionText = "これはテスト用の文字起こしテキストです。"
        val memo = VoiceMemo.create(id = memoId, userId = userId)
            .completeTranscription(transcriptionText)

        val voiceMemoRepository = InMemoryVoiceMemoRepository(
            initialMemos = listOf(memo),
        )
        val useCase = GetTranscriptionUseCase(voiceMemoRepository)

        val result = useCase.execute(GetTranscriptionInput(memoId = memoId, userId = userId))

        assertEquals(transcriptionText, result.transcription)
    }

    @Test
    fun `他人のメモを取得しようとするとMEMO_NOT_FOUNDエラーになる`() = runTest {
        val ownerId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        val memoId = UUID.randomUUID()
        val memo = VoiceMemo.create(id = memoId, userId = ownerId)
            .completeTranscription("テスト")

        val voiceMemoRepository = InMemoryVoiceMemoRepository(
            initialMemos = listOf(memo),
        )
        val useCase = GetTranscriptionUseCase(voiceMemoRepository)

        val exception = assertThrows<DomainException> {
            useCase.execute(GetTranscriptionInput(memoId = memoId, userId = otherUserId))
        }

        // メモの存在を推測されないよう、403ではなく404を返す
        assertEquals(ErrorCode.MEMO_NOT_FOUND, exception.code)
    }

    @Test
    fun `存在しないメモを取得しようとするとMEMO_NOT_FOUNDエラーになる`() = runTest {
        val userId = UUID.randomUUID()
        val nonExistentMemoId = UUID.randomUUID()

        val voiceMemoRepository = InMemoryVoiceMemoRepository()
        val useCase = GetTranscriptionUseCase(voiceMemoRepository)

        val exception = assertThrows<DomainException> {
            useCase.execute(GetTranscriptionInput(memoId = nonExistentMemoId, userId = userId))
        }

        assertEquals(ErrorCode.MEMO_NOT_FOUND, exception.code)
    }

    @Test
    fun `削除済みのメモを取得しようとするとMEMO_NOT_FOUNDエラーになる`() = runTest {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()
        val memo = VoiceMemo.create(id = memoId, userId = userId)
            .completeTranscription("テスト")
            .markAsDeleted()

        val voiceMemoRepository = InMemoryVoiceMemoRepository(
            initialMemos = listOf(memo),
        )
        val useCase = GetTranscriptionUseCase(voiceMemoRepository)

        val exception = assertThrows<DomainException> {
            useCase.execute(GetTranscriptionInput(memoId = memoId, userId = userId))
        }

        assertEquals(ErrorCode.MEMO_NOT_FOUND, exception.code)
    }

    @Test
    fun `文字起こしが処理中のメモを取得しようとするとTRANSCRIPTION_NOT_COMPLETEDエラーになる`() = runTest {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()
        // create直後は PENDING 状態
        val memo = VoiceMemo.create(id = memoId, userId = userId)

        val voiceMemoRepository = InMemoryVoiceMemoRepository(
            initialMemos = listOf(memo),
        )
        val useCase = GetTranscriptionUseCase(voiceMemoRepository)

        val exception = assertThrows<DomainException> {
            useCase.execute(GetTranscriptionInput(memoId = memoId, userId = userId))
        }

        assertEquals(ErrorCode.TRANSCRIPTION_NOT_COMPLETED, exception.code)
    }

    @Test
    fun `文字起こしが失敗したメモを取得しようとするとTRANSCRIPTION_FAILEDエラーになる`() = runTest {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()
        val memo = VoiceMemo.create(id = memoId, userId = userId)
            .failTranscription()

        val voiceMemoRepository = InMemoryVoiceMemoRepository(
            initialMemos = listOf(memo),
        )
        val useCase = GetTranscriptionUseCase(voiceMemoRepository)

        val exception = assertThrows<DomainException> {
            useCase.execute(GetTranscriptionInput(memoId = memoId, userId = userId))
        }

        assertEquals(ErrorCode.TRANSCRIPTION_FAILED, exception.code)
    }
}
