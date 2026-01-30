package com.assari.voicebooklm.presentation.controller.memo

import com.assari.voicebooklm.domain.exception.DomainException
import com.assari.voicebooklm.domain.exception.ErrorCode
import com.assari.voicebooklm.domain.model.VoiceMemo
import com.assari.voicebooklm.usecase.memo.DeleteMemoInput
import com.assari.voicebooklm.usecase.memo.DeleteMemoUseCase
import com.assari.voicebooklm.usecase.memo.GetMemoInput
import com.assari.voicebooklm.usecase.memo.GetMemoOutput
import com.assari.voicebooklm.usecase.memo.GetMemoUseCase
import com.assari.voicebooklm.usecase.memo.GetTranscriptionInput
import com.assari.voicebooklm.usecase.memo.GetTranscriptionOutput
import com.assari.voicebooklm.usecase.memo.GetTranscriptionUseCase
import com.assari.voicebooklm.usecase.memo.ListMemosInput
import com.assari.voicebooklm.usecase.memo.ListMemosOutput
import com.assari.voicebooklm.usecase.memo.ListMemosUseCase
import com.assari.voicebooklm.usecase.memo.MemoWithFolder
import com.assari.voicebooklm.usecase.memo.ResummarizeUseCase
import com.assari.voicebooklm.usecase.memo.FormatMemoInput
import com.assari.voicebooklm.usecase.memo.FormatMemoOutput
import com.assari.voicebooklm.usecase.memo.FormatMemoUseCase
import com.assari.voicebooklm.usecase.memo.FormatProcessingTime
import com.assari.voicebooklm.usecase.memo.UpdateMemoInput
import com.assari.voicebooklm.usecase.memo.UpdateMemoOutput
import com.assari.voicebooklm.usecase.memo.UpdateMemoUseCase
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.mockk
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

/**
 * MemoController の一覧取得と認証判定を直接呼び出しで検証。
 */
class MemoControllerTest {

    private lateinit var listMemosUseCase: ListMemosUseCase
    private lateinit var getMemoUseCase: GetMemoUseCase
    private lateinit var getTranscriptionUseCase: GetTranscriptionUseCase
    private lateinit var updateMemoUseCase: UpdateMemoUseCase
    private lateinit var deleteMemoUseCase: DeleteMemoUseCase
    private lateinit var resummarizeUseCase: ResummarizeUseCase
    private lateinit var formatMemoUseCase: FormatMemoUseCase
    private lateinit var controller: MemoController

    @BeforeEach
    fun setup() {
        listMemosUseCase = mockk()
        getMemoUseCase = mockk()
        getTranscriptionUseCase = mockk()
        updateMemoUseCase = mockk()
        deleteMemoUseCase = mockk()
        resummarizeUseCase = mockk()
        formatMemoUseCase = mockk()
        controller = MemoController(
            listMemosUseCase = listMemosUseCase,
            getMemoUseCase = getMemoUseCase,
            getTranscriptionUseCase = getTranscriptionUseCase,
            updateMemoUseCase = updateMemoUseCase,
            deleteMemoUseCase = deleteMemoUseCase,
            resummarizeUseCase = resummarizeUseCase,
            formatMemoUseCase = formatMemoUseCase,
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
            keyword = null,
        )
        coEvery { listMemosUseCase.execute(input) } returns ListMemosOutput(
            memos = listOf(
                MemoWithFolder(memo = completedMemo, folder = null, folderPath = null),
                MemoWithFolder(memo = pendingMemo, folder = null, folderPath = null),
            ),
            total = 2,
            hasMore = false,
        )

        val response = controller.listMemos(userId, null, false, false, null, null, "updated_at", "desc", null, null)

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
            keyword = null,
        )
        coEvery { listMemosUseCase.execute(input) } returns ListMemosOutput(
            memos = emptyList(),
            total = 0,
            hasMore = false,
        )

        val response = controller.listMemos(userId, null, false, false, null, null, "updated_at", "desc", null, null)

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = requireNotNull(response.body) { "response body should not be null" }
        assertTrue(body.memos.isEmpty())
    }

    @Test
    fun `listMemos 未認証の場合はResponseStatusExceptionがスローされる`() = runBlocking {
        val exception = assertThrows<ResponseStatusException> {
            controller.listMemos(null, null, false, false, null, null, "updated_at", "desc", null, null)
        }

        assertEquals(HttpStatus.UNAUTHORIZED, exception.statusCode)
    }

    @Test
    fun `listMemos limitが0の場合はBAD_REQUESTがスローされる`() = runBlocking {
        val userId = UUID.randomUUID()
        val exception = assertThrows<ResponseStatusException> {
            controller.listMemos(userId, null, false, false, null, null, "updated_at", "desc", 0, null)
        }

        assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
        assertEquals("limitは1以上の値を指定してください", exception.reason)
    }

    @Test
    fun `listMemos limitが負の値の場合はBAD_REQUESTがスローされる`() = runBlocking {
        val userId = UUID.randomUUID()
        val exception = assertThrows<ResponseStatusException> {
            controller.listMemos(userId, null, false, false, null, null, "updated_at", "desc", -1, null)
        }

        assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
        assertEquals("limitは1以上の値を指定してください", exception.reason)
    }

    @Test
    fun `listMemos offsetが負の値の場合はBAD_REQUESTがスローされる`() = runBlocking {
        val userId = UUID.randomUUID()
        val exception = assertThrows<ResponseStatusException> {
            controller.listMemos(userId, null, false, false, null, null, "updated_at", "desc", null, -1)
        }

        assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
        assertEquals("offsetは0以上の値を指定してください", exception.reason)
    }

    @Test
    fun `listMemos レスポンスにtotalとhasMoreが含まれる`() = runBlocking {
        val userId = UUID.randomUUID()
        val memo1 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
            .completeTranscription("text")
            .completeFormatting(title = "title1", content = "content1", tags = emptyList())
        val memo2 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
            .completeTranscription("text")
            .completeFormatting(title = "title2", content = "content2", tags = emptyList())
        val input = ListMemosInput(
            userId = userId,
            folderId = null,
            includeDescendants = false,
            uncategorizedOnly = false,
            keyword = null,
        )
        coEvery { listMemosUseCase.execute(input) } returns ListMemosOutput(
            memos = listOf(
                MemoWithFolder(memo = memo1, folder = null, folderPath = null),
            ),
            total = 2,
            hasMore = true,
        )

        val response = controller.listMemos(userId, null, false, false, null, null, "updated_at", "desc", null, null)

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = requireNotNull(response.body) { "response body should not be null" }
        assertEquals(1, body.memos.size)
        assertEquals(2, body.total)
        assertTrue(body.hasMore)
    }

    // ===== getMemo エンドポイントのテスト =====

    @Test
    fun `認証済みユーザーが自分のメモ詳細を取得すると200が返る`() = runBlocking {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()
        val memo = VoiceMemo.create(id = memoId, userId = userId)
            .completeTranscription("transcription text")
            .completeFormatting(
                title = "テストタイトル",
                content = "テスト本文",
                tags = listOf("tag1", "tag2"),
            )

        coEvery { getMemoUseCase.execute(GetMemoInput(memoId, userId)) } returns GetMemoOutput(memo, null, null)

        val response = controller.getMemo(memoId, userId)

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = requireNotNull(response.body) { "response body should not be null" }
        assertEquals(memoId, body.memoId)
        assertEquals("テストタイトル", body.title)
        assertEquals("テスト本文", body.content)
        assertEquals(listOf("tag1", "tag2"), body.tags)
        assertEquals("transcription text", body.transcriptionText)
        assertEquals("COMPLETED", body.transcriptionStatus)
        assertEquals("COMPLETED", body.formattingStatus)
    }

    @Test
    fun `getMemo 整形未完了のメモを取得するとnullのフィールドが含まれる`() = runBlocking {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()
        val memo = VoiceMemo.create(id = memoId, userId = userId)

        coEvery { getMemoUseCase.execute(GetMemoInput(memoId, userId)) } returns GetMemoOutput(memo, null, null)

        val response = controller.getMemo(memoId, userId)

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = requireNotNull(response.body) { "response body should not be null" }
        assertEquals(memoId, body.memoId)
        assertNull(body.title)
        assertNull(body.content)
        assertTrue(body.tags.isEmpty())
        assertNull(body.transcriptionText)
        assertEquals("PENDING", body.transcriptionStatus)
        assertEquals("PENDING", body.formattingStatus)
    }

    @Test
    fun `getMemo 未認証の場合はResponseStatusExceptionがスローされる`() = runBlocking {
        val memoId = UUID.randomUUID()

        val exception = assertThrows<ResponseStatusException> {
            controller.getMemo(memoId, null)
        }

        assertEquals(HttpStatus.UNAUTHORIZED, exception.statusCode)
    }

    @Test
    fun `getMemo 他人のメモを取得しようとするとMEMO_NOT_FOUND例外がスローされる`() = runBlocking {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()

        // メモの存在を推測されないよう、403ではなく404を返す
        coEvery { getMemoUseCase.execute(GetMemoInput(memoId, userId)) } throws
            DomainException(ErrorCode.MEMO_NOT_FOUND)

        val exception = assertThrows<DomainException> {
            controller.getMemo(memoId, userId)
        }

        assertEquals(ErrorCode.MEMO_NOT_FOUND, exception.code)
    }

    @Test
    fun `getMemo 存在しないメモIDを指定するとMEMO_NOT_FOUND例外がスローされる`() = runBlocking {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()

        coEvery { getMemoUseCase.execute(GetMemoInput(memoId, userId)) } throws
            DomainException(ErrorCode.MEMO_NOT_FOUND)

        val exception = assertThrows<DomainException> {
            controller.getMemo(memoId, userId)
        }

        assertEquals(ErrorCode.MEMO_NOT_FOUND, exception.code)
    }

    @Test
    fun `getMemo 削除済みのメモを取得しようとするとMEMO_NOT_FOUND例外がスローされる`() = runBlocking {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()

        coEvery { getMemoUseCase.execute(GetMemoInput(memoId, userId)) } throws
            DomainException(ErrorCode.MEMO_NOT_FOUND)

        val exception = assertThrows<DomainException> {
            controller.getMemo(memoId, userId)
        }

        assertEquals(ErrorCode.MEMO_NOT_FOUND, exception.code)
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
    fun `deleteMemo 未認証の場合はResponseStatusExceptionがスローされる`() = runBlocking {
        val memoId = UUID.randomUUID()

        val exception = assertThrows<ResponseStatusException> {
            controller.deleteMemo(memoId, null)
        }

        assertEquals(HttpStatus.UNAUTHORIZED, exception.statusCode)
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

    // ===== updateMemo エンドポイントのテスト =====

    @Test
    fun `認証済みユーザーが自分のメモを更新すると更新されたメモが返る`() = runBlocking {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()
        val updatedMemo = VoiceMemo.create(id = memoId, userId = userId)
            .startTranscription()
            .completeTranscription("transcription text")
            .startFormatting()
            .completeFormatting(
                title = "更新後タイトル",
                content = "更新後本文",
                tags = listOf("new1", "new2"),
            )

        val request = UpdateMemoRequest(
            title = "更新後タイトル",
            content = null,
            tags = null,
        )

        coEvery {
            updateMemoUseCase.execute(
                UpdateMemoInput(
                    memoId = memoId,
                    userId = userId,
                    title = "更新後タイトル",
                    content = null,
                    tags = null,
                )
            )
        } returns UpdateMemoOutput(updatedMemo)

        val response = controller.updateMemo(memoId, userId, request)

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = requireNotNull(response.body) { "response body should not be null" }
        assertEquals(memoId, body.memoId)
        assertEquals("更新後タイトル", body.title)
        assertEquals("更新後本文", body.content)
        assertEquals(listOf("new1", "new2"), body.tags)
    }

    @Test
    fun `updateMemo 未認証の場合はResponseStatusExceptionがスローされる`() = runBlocking {
        val memoId = UUID.randomUUID()
        val request = UpdateMemoRequest(title = "新タイトル")

        val exception = assertThrows<ResponseStatusException> {
            controller.updateMemo(memoId, null, request)
        }

        assertEquals(HttpStatus.UNAUTHORIZED, exception.statusCode)
    }

    @Test
    fun `updateMemo 他人のメモを更新しようとするとMEMO_NOT_FOUND例外がスローされる`() = runBlocking {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()
        val request = UpdateMemoRequest(title = "新タイトル")

        coEvery {
            updateMemoUseCase.execute(
                UpdateMemoInput(
                    memoId = memoId,
                    userId = userId,
                    title = "新タイトル",
                    content = null,
                    tags = null,
                )
            )
        } throws DomainException(ErrorCode.MEMO_NOT_FOUND)

        val exception = assertThrows<DomainException> {
            controller.updateMemo(memoId, userId, request)
        }

        assertEquals(ErrorCode.MEMO_NOT_FOUND, exception.code)
    }

    @Test
    fun `updateMemo 整形未完了のメモを更新しようとするとMEMO_NOT_COMPLETED例外がスローされる`() = runBlocking {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()
        val request = UpdateMemoRequest(title = "新タイトル")

        coEvery {
            updateMemoUseCase.execute(
                UpdateMemoInput(
                    memoId = memoId,
                    userId = userId,
                    title = "新タイトル",
                    content = null,
                    tags = null,
                )
            )
        } throws DomainException(ErrorCode.MEMO_NOT_COMPLETED)

        val exception = assertThrows<DomainException> {
            controller.updateMemo(memoId, userId, request)
        }

        assertEquals(ErrorCode.MEMO_NOT_COMPLETED, exception.code)
    }

    @Test
    fun `updateMemo 存在しないメモIDを指定するとMEMO_NOT_FOUND例外がスローされる`() = runBlocking {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()
        val request = UpdateMemoRequest(title = "新タイトル")

        coEvery {
            updateMemoUseCase.execute(
                UpdateMemoInput(
                    memoId = memoId,
                    userId = userId,
                    title = "新タイトル",
                    content = null,
                    tags = null,
                )
            )
        } throws DomainException(ErrorCode.MEMO_NOT_FOUND)

        val exception = assertThrows<DomainException> {
            controller.updateMemo(memoId, userId, request)
        }

        assertEquals(ErrorCode.MEMO_NOT_FOUND, exception.code)
    }

    // ===== getTranscription エンドポイントのテスト =====

    @Test
    fun `認証済みユーザーが自分のメモの文字起こしテキストを取得すると200が返る`() = runBlocking {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()
        val transcriptionText = "これはテスト用の文字起こしテキストです。"

        coEvery {
            getTranscriptionUseCase.execute(GetTranscriptionInput(memoId, userId))
        } returns GetTranscriptionOutput(transcriptionText)

        val response = controller.getTranscription(memoId, userId)

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = requireNotNull(response.body) { "response body should not be null" }
        assertEquals(transcriptionText, body.transcription)
    }

    @Test
    fun `getTranscription 未認証の場合はResponseStatusExceptionがスローされる`() = runBlocking {
        val memoId = UUID.randomUUID()

        val exception = assertThrows<ResponseStatusException> {
            controller.getTranscription(memoId, null)
        }

        assertEquals(HttpStatus.UNAUTHORIZED, exception.statusCode)
    }

    @Test
    fun `getTranscription 他人のメモを取得しようとするとMEMO_NOT_FOUND例外がスローされる`() = runBlocking {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()

        coEvery {
            getTranscriptionUseCase.execute(GetTranscriptionInput(memoId, userId))
        } throws DomainException(ErrorCode.MEMO_NOT_FOUND)

        val exception = assertThrows<DomainException> {
            controller.getTranscription(memoId, userId)
        }

        assertEquals(ErrorCode.MEMO_NOT_FOUND, exception.code)
    }

    @Test
    fun `getTranscription 存在しないメモIDを指定するとMEMO_NOT_FOUND例外がスローされる`() = runBlocking {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()

        coEvery {
            getTranscriptionUseCase.execute(GetTranscriptionInput(memoId, userId))
        } throws DomainException(ErrorCode.MEMO_NOT_FOUND)

        val exception = assertThrows<DomainException> {
            controller.getTranscription(memoId, userId)
        }

        assertEquals(ErrorCode.MEMO_NOT_FOUND, exception.code)
    }

    @Test
    fun `getTranscription 文字起こしが未完了のメモを取得しようとするとTRANSCRIPTION_NOT_COMPLETED例外がスローされる`() = runBlocking {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()

        coEvery {
            getTranscriptionUseCase.execute(GetTranscriptionInput(memoId, userId))
        } throws DomainException(ErrorCode.TRANSCRIPTION_NOT_COMPLETED)

        val exception = assertThrows<DomainException> {
            controller.getTranscription(memoId, userId)
        }

        assertEquals(ErrorCode.TRANSCRIPTION_NOT_COMPLETED, exception.code)
    }

    @Test
    fun `getTranscription 文字起こしが失敗したメモを取得しようとするとTRANSCRIPTION_FAILED例外がスローされる`() = runBlocking {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()

        coEvery {
            getTranscriptionUseCase.execute(GetTranscriptionInput(memoId, userId))
        } throws DomainException(ErrorCode.TRANSCRIPTION_FAILED)

        val exception = assertThrows<DomainException> {
            controller.getTranscription(memoId, userId)
        }

        assertEquals(ErrorCode.TRANSCRIPTION_FAILED, exception.code)
    }

    // ===== formatMemo エンドポイントのテスト =====

    @Test
    fun `認証済みユーザーがformatMemoを呼ぶと201が返る`() = runBlocking {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()
        val transcriptionText = "これは文字起こしテキストです"

        val memo = VoiceMemo.create(id = memoId, userId = userId)
            .startTranscription()
            .completeTranscription(transcriptionText)
            .startFormatting()
            .completeFormatting(
                title = "整形されたタイトル",
                content = "整形された本文",
                tags = listOf("タグ1", "タグ2"),
            )

        coEvery {
            formatMemoUseCase.execute(
                FormatMemoInput(
                    userId = userId,
                    transcription = transcriptionText,
                    language = "ja-JP",
                )
            )
        } returns FormatMemoOutput(
            voiceMemo = memo,
            processingTime = FormatProcessingTime(
                formatting = 200.milliseconds,
                persistence = 50.milliseconds,
                total = 250.milliseconds,
            ),
        )

        val request = FormatMemoRequest(
            transcription = transcriptionText,
            language = "ja-JP",
        )

        val response = controller.formatMemo(userId, request)

        assertEquals(HttpStatus.CREATED, response.statusCode)
        val body = requireNotNull(response.body) { "response body should not be null" }
        assertEquals(memoId, body.memoId)
        assertEquals("整形されたタイトル", body.title)
        assertEquals("整形された本文", body.content)
        assertEquals(listOf("タグ1", "タグ2"), body.tags)
        assertEquals("COMPLETED", body.formattingStatus)
    }

    @Test
    fun `formatMemo 未認証の場合はResponseStatusExceptionがスローされる`() = runBlocking {
        val request = FormatMemoRequest(
            transcription = "テスト",
            language = null,
        )

        val exception = assertThrows<ResponseStatusException> {
            controller.formatMemo(null, request)
        }

        assertEquals(HttpStatus.UNAUTHORIZED, exception.statusCode)
    }

    @Test
    fun `formatMemo ユースケースがIllegalArgumentExceptionをスローするとBAD_REQUESTが返る`() = runBlocking {
        val userId = UUID.randomUUID()
        val request = FormatMemoRequest(
            transcription = "テスト",
            language = null,
        )

        coEvery {
            formatMemoUseCase.execute(any())
        } throws IllegalArgumentException("文字起こしテキストが空です")

        val exception = assertThrows<ResponseStatusException> {
            controller.formatMemo(userId, request)
        }

        assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
    }

    @Test
    fun `formatMemo 言語コード未指定でも正常に動作する`() = runBlocking {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()
        val transcriptionText = "テスト"

        val memo = VoiceMemo.create(id = memoId, userId = userId)
            .startTranscription()
            .completeTranscription(transcriptionText)
            .startFormatting()
            .completeFormatting(
                title = "タイトル",
                content = "本文",
                tags = emptyList(),
            )

        coEvery {
            formatMemoUseCase.execute(
                FormatMemoInput(
                    userId = userId,
                    transcription = transcriptionText,
                    language = null,
                )
            )
        } returns FormatMemoOutput(
            voiceMemo = memo,
            processingTime = FormatProcessingTime(
                formatting = 100.milliseconds,
                persistence = 30.milliseconds,
                total = 130.milliseconds,
            ),
        )

        val request = FormatMemoRequest(
            transcription = transcriptionText,
            language = null,
        )

        val response = controller.formatMemo(userId, request)

        assertEquals(HttpStatus.CREATED, response.statusCode)
        val body = requireNotNull(response.body) { "response body should not be null" }
        assertEquals(memoId, body.memoId)
    }

    @Test
    fun `formatMemo に folderId, folderPath, tags を指定した場合に use case に正しく渡り 201 が返る`() = runBlocking {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()
        val folderId = UUID.randomUUID()
        val folderPath = "仕事/テスト"
        val tags = listOf("ユーザータグ1", "ユーザータグ2")
        val transcriptionText = "文字起こしテキスト"

        val memo = VoiceMemo.create(id = memoId, userId = userId)
            .startTranscription()
            .completeTranscription(transcriptionText)
            .startFormatting()
            .completeFormatting(
                title = "タイトル",
                content = "本文",
                tags = listOf("ユーザータグ1", "ユーザータグ2", "AIタグ"),
            )

        coEvery {
            formatMemoUseCase.execute(
                match {
                    it.userId == userId &&
                        it.transcription == transcriptionText &&
                        it.language == "ja-JP" &&
                        it.folderId == folderId &&
                        it.folderPath == folderPath &&
                        it.tags == tags
                },
            )
        } returns FormatMemoOutput(
            voiceMemo = memo,
            processingTime = FormatProcessingTime(
                formatting = 100.milliseconds,
                persistence = 30.milliseconds,
                total = 130.milliseconds,
            ),
        )

        val request = FormatMemoRequest(
            transcription = transcriptionText,
            language = "ja-JP",
            folderId = folderId,
            folderPath = folderPath,
            tags = tags,
        )

        val response = controller.formatMemo(userId, request)

        assertEquals(HttpStatus.CREATED, response.statusCode)
        val body = requireNotNull(response.body) { "response body should not be null" }
        assertEquals(memoId, body.memoId)
        assertEquals("タイトル", body.title)
        assertEquals(listOf("ユーザータグ1", "ユーザータグ2", "AIタグ"), body.tags)
    }

    @Test
    fun `formatMemo で folderId 指定時に FOLDER_NOT_FOUND の場合は DomainException がスローされる`() = runBlocking {
        val userId = UUID.randomUUID()
        val folderId = UUID.randomUUID()
        val request = FormatMemoRequest(
            transcription = "テスト",
            language = null,
            folderId = folderId,
            folderPath = null,
            tags = null,
        )

        coEvery {
            formatMemoUseCase.execute(any())
        } throws DomainException(ErrorCode.FOLDER_NOT_FOUND, "フォルダーが見つかりません: $folderId")

        val exception = assertThrows<DomainException> {
            controller.formatMemo(userId, request)
        }

        assertEquals(ErrorCode.FOLDER_NOT_FOUND, exception.code)
    }
}
