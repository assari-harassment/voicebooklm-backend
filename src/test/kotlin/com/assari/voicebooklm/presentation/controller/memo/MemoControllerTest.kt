package com.assari.voicebooklm.presentation.controller.memo

import com.assari.voicebooklm.domain.exception.DomainException
import com.assari.voicebooklm.domain.exception.ErrorCode
import com.assari.voicebooklm.domain.model.VoiceMemo
import com.assari.voicebooklm.usecase.memo.DeleteMemoInput
import com.assari.voicebooklm.usecase.memo.DeleteMemoUseCase
import com.assari.voicebooklm.usecase.memo.ListMemosInput
import com.assari.voicebooklm.usecase.memo.ListMemosOutput
import com.assari.voicebooklm.usecase.memo.ListMemosUseCase
import com.assari.voicebooklm.usecase.memo.MemoWithFolder
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.mockk
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus

/**
 * MemoController の一覧取得と認証判定を直接呼び出しで検証。
 */
class MemoControllerTest {

    private lateinit var listMemosUseCase: ListMemosUseCase
    private lateinit var deleteMemoUseCase: DeleteMemoUseCase
    private lateinit var controller: MemoController

    @BeforeEach
    fun setup() {
        listMemosUseCase = mockk()
        deleteMemoUseCase = mockk()
        controller = MemoController(
            listMemosUseCase = listMemosUseCase,
            deleteMemoUseCase = deleteMemoUseCase,
        )
    }

    @Test
    fun `認証済みユーザーのメモ一覧を返す`() = runBlocking {
        val userId = UUID.randomUUID()
        // 整形済みメモと未整形メモを混在させてレスポンス形式を確認する。
        val completedMemo = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
            .completeTranscription("text")
            .completeFormatting(
                title = "title",
                content = "content",
                tags = listOf("t1"),
            )
        val pendingMemo = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
        val input = ListMemosInput(
            userId = userId,
            folderId = null,
            includeDescendants = false,
            uncategorizedOnly = false,
        )
        coEvery { listMemosUseCase.execute(input) } returns ListMemosOutput(
            memos = listOf(
                MemoWithFolder(memo = completedMemo, folder = null, folderPath = null),
                MemoWithFolder(memo = pendingMemo, folder = null, folderPath = null),
            ),
        )

        val response = controller.listMemos(userId, null, false, false)

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = requireNotNull(response.body) { "response body should not be null" }
        assertEquals(2, body.memos.size)
        assertEquals(completedMemo.id, body.memos[0].memoId)
        assertEquals("title", body.memos[0].title)
        assertEquals(listOf("t1"), body.memos[0].tags)
        assertEquals(pendingMemo.id, body.memos[1].memoId)
        assertNull(body.memos[1].title)
    }

    @Test
    fun `メモが存在しない場合は空リストを返す`() = runBlocking {
        val userId = UUID.randomUUID()
        // ユースケースの戻り値が空配列のときのレスポンスを検証する。
        val input = ListMemosInput(
            userId = userId,
            folderId = null,
            includeDescendants = false,
            uncategorizedOnly = false,
        )
        coEvery { listMemosUseCase.execute(input) } returns ListMemosOutput(
            memos = emptyList(),
        )

        val response = controller.listMemos(userId, null, false, false)

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = requireNotNull(response.body) { "response body should not be null" }
        assertTrue(body.memos.isEmpty())
    }

    @Test
    fun `未認証は401`() = runBlocking {
        // 認証情報がない場合は Unauthorized を返す。
        val response = controller.listMemos(null, null, false, false)

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertNull(response.body)
    }

    // ===== deleteMemo エンドポイントのテスト =====

    @Test
    fun `認証済みユーザーが自分のメモを削除すると204が返る`() = runBlocking {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()
        // ユースケースが正常に終了する場合
        coJustRun { deleteMemoUseCase.execute(DeleteMemoInput(memoId, userId)) }

        val response = controller.deleteMemo(memoId, userId)

        assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
        assertNull(response.body)
    }

    @Test
    fun `deleteMemo 未認証の場合は401が返る`() = runBlocking {
        val memoId = UUID.randomUUID()

        val response = controller.deleteMemo(memoId, null)

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertNull(response.body)
    }

    @Test
    fun `deleteMemo 他人のメモを削除しようとするとUNAUTHORIZED_ACCESS例外がスローされる`() = runBlocking {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()
        // ユースケースが UNAUTHORIZED_ACCESS エラーをスローする
        // 実際のリクエストではGlobalExceptionHandlerが403を返す
        coEvery { deleteMemoUseCase.execute(DeleteMemoInput(memoId, userId)) } throws
            DomainException(ErrorCode.UNAUTHORIZED_ACCESS)

        val exception = assertThrows<DomainException> {
            controller.deleteMemo(memoId, userId)
        }

        assertEquals(ErrorCode.UNAUTHORIZED_ACCESS, exception.code)
    }

    @Test
    fun `deleteMemo 存在しないメモIDを指定するとMEMO_NOT_FOUND例外がスローされる`() = runBlocking {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()
        // ユースケースが MEMO_NOT_FOUND エラーをスローする
        // 実際のリクエストではGlobalExceptionHandlerが404を返す
        coEvery { deleteMemoUseCase.execute(DeleteMemoInput(memoId, userId)) } throws
            DomainException(ErrorCode.MEMO_NOT_FOUND)

        val exception = assertThrows<DomainException> {
            controller.deleteMemo(memoId, userId)
        }

        assertEquals(ErrorCode.MEMO_NOT_FOUND, exception.code)
    }
}
