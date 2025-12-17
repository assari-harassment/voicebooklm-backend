package com.assari.voicebooklm.presentation.controller.voice

import com.assari.voicebooklm.domain.model.Memo
import com.assari.voicebooklm.domain.repository.MemoRepository
import com.assari.voicebooklm.presentation.exception.ErrorResponse
import com.assari.voicebooklm.usecase.memo.CreateMemoCommand
import com.assari.voicebooklm.usecase.memo.CreateMemoResult
import com.assari.voicebooklm.usecase.memo.CreateMemoUseCase
import com.assari.voicebooklm.usecase.memo.FallbackUsage
import com.assari.voicebooklm.usecase.memo.ProcessingTime
import com.assari.voicebooklm.usecase.memo.client.AiMemoFormatter
import com.assari.voicebooklm.usecase.memo.client.SpeechTranscriber
import com.assari.voicebooklm.usecase.memo.client.SpeechTranscription
import com.assari.voicebooklm.usecase.support.ExecutionTimer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.web.server.ResponseStatusException

/**
 * VoiceController の入力検証とユースケース呼び出しを直接呼び出しでテスト。
 */
class VoiceControllerTest {

    private lateinit var createMemoUseCase: CreateMemoUseCase
    private lateinit var memoRepository: MemoRepository
    private lateinit var speechTranscriber: SpeechTranscriber
    private lateinit var aiMemoFormatter: AiMemoFormatter
    private lateinit var executionTimer: ExecutionTimer
    private lateinit var controller: VoiceController

    @BeforeEach
    fun setup() {
        createMemoUseCase = mockk()
        memoRepository = mockk(relaxed = true)
        speechTranscriber = mockk(relaxed = true)
        aiMemoFormatter = mockk(relaxed = true)
        executionTimer = mockk(relaxed = true)
        // テストではユースケース生成を差し替えるため、Compose 済みのモックを渡す
        controller = VoiceController(
            memoRepository = memoRepository,
            speechTranscriber = speechTranscriber,
            aiMemoFormatter = aiMemoFormatter,
            executionTimer = executionTimer,
            createMemoUseCaseOverride = createMemoUseCase,
        )
    }

    @Test
    fun `正常にメモを生成し 201 を返す`() = runBlocking {
        val memoId = UUID.randomUUID()
        val userId = UUID.fromString("11111111-1111-1111-1111-111111111111")
        coEvery { createMemoUseCase.execute(any()) } returns stubResult(memoId, userId)

        val file = MockMultipartFile(
            "file",
            "voice.wav",
            "audio/wav",
            byteArrayOf(1, 2, 3),
        )

        val response: ResponseEntity<CreateMemoResponse> = controller.createMemo(
            authentication = UsernamePasswordAuthenticationToken(userId.toString(), "pw"),
            file = file,
            language = "ja-JP",
        )

        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertEquals(memoId, response.body?.memoId)
        assertEquals("title", response.body?.title)
        assertEquals(false, response.body?.fallback?.transcription)
        assertEquals(false, response.body?.fallback?.formatting)

        coVerify {
            createMemoUseCase.execute(
                match {
                    it.userId == userId &&
                        it.audio.isNotEmpty() &&
                        it.audioMimeType == "audio/wav" &&
                        it.language == "ja-JP"
                },
            )
        }
    }

    @Test
    fun `未認証は 401`() {
        val file = MockMultipartFile(
            "file",
            "voice.wav",
            "audio/wav",
            byteArrayOf(1, 2, 3),
        )

        val ex = assertThrows(ResponseStatusException::class.java) {
            runBlocking {
                controller.createMemo(
                    authentication = null,
                    file = file,
                    language = null,
                )
            }
        }
        assertEquals(HttpStatus.UNAUTHORIZED, ex.statusCode)
    }

    @Test
    fun `非音声コンテンツタイプは 400`() {
        val file = MockMultipartFile(
            "file",
            "text.txt",
            "text/plain",
            byteArrayOf(1, 2, 3),
        )

        val ex = assertThrows(ResponseStatusException::class.java) {
            runBlocking {
                controller.createMemo(
                    authentication = UsernamePasswordAuthenticationToken("11111111-1111-1111-1111-111111111111", "pw"),
                    file = file,
                    language = null,
                )
            }
        }
        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
    }

    @Test
    fun `空ファイルは 400`() {
        val file = MockMultipartFile(
            "file",
            "voice.wav",
            "audio/wav",
            ByteArray(0),
        )

        val ex = assertThrows(ResponseStatusException::class.java) {
            runBlocking {
                controller.createMemo(
                    authentication = UsernamePasswordAuthenticationToken("11111111-1111-1111-1111-111111111111", "pw"),
                    file = file,
                    language = null,
                )
            }
        }
        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
    }

    private fun stubResult(memoId: UUID, userId: UUID) = CreateMemoResult(
        memo = Memo.create(
            title = "title",
            content = "content",
            tags = listOf("t1"),
            userId = userId,
            id = memoId,
        ),
        transcription = SpeechTranscription(
            text = "text",
            languageCode = "ja-JP",
        ),
        processingTime = ProcessingTime(
            transcription = 10.milliseconds,
            formatting = 20.milliseconds,
            persistence = 30.milliseconds,
            total = Duration.parse("0.06s"),
        ),
        fallbackUsage = FallbackUsage(
            transcription = false,
            formatting = false,
        ),
    )
}
