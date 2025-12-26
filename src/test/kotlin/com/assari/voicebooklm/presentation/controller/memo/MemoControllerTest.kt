package com.assari.voicebooklm.presentation.controller.memo

import com.assari.voicebooklm.domain.model.VoiceMemo
import com.assari.voicebooklm.usecase.memo.ListMemosInput
import com.assari.voicebooklm.usecase.memo.ListMemosOutput
import com.assari.voicebooklm.usecase.memo.ListMemosUseCase
import io.mockk.coEvery
import io.mockk.mockk
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

/**
 * MemoController の一覧取得と認証判定を直接呼び出しで検証。
 */
class MemoControllerTest {

    private lateinit var listMemosUseCase: ListMemosUseCase
    private lateinit var controller: MemoController

    @BeforeEach
    fun setup() {
        listMemosUseCase = mockk()
        controller = MemoController(
            listMemosUseCase = listMemosUseCase,
        )
    }

    @Test
    fun `認証済みユーザーのメモ一覧を返す`() = runBlocking {
        val userId = UUID.randomUUID()
        // 整形済みメモと未整形メモを混在させてレスポンス形式を確認する。
        val completedMemo = VoiceMemo.create(userId = userId)
            .completeTranscription("text")
            .completeFormatting(
                title = "title",
                content = "content",
                tags = listOf("t1"),
            )
        val pendingMemo = VoiceMemo.create(userId = userId)
        coEvery { listMemosUseCase.execute(ListMemosInput(userId)) } returns ListMemosOutput(
            memos = listOf(completedMemo, pendingMemo),
        )

        val response = controller.listMemos(userId)

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
        coEvery { listMemosUseCase.execute(ListMemosInput(userId)) } returns ListMemosOutput(
            memos = emptyList(),
        )

        val response = controller.listMemos(userId)

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = requireNotNull(response.body) { "response body should not be null" }
        assertTrue(body.memos.isEmpty())
    }

    @Test
    fun `未認証は401`() = runBlocking {
        // 認証情報がない場合は Unauthorized を返す。
        val response = controller.listMemos(null)

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertNull(response.body)
    }
}
