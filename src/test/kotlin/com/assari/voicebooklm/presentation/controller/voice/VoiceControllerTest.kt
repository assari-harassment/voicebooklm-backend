package com.assari.voicebooklm.presentation.controller.voice

import com.assari.voicebooklm.domain.model.Formatting
import com.assari.voicebooklm.domain.model.Transcription
import com.assari.voicebooklm.domain.model.VoiceMemo
import com.assari.voicebooklm.usecase.memo.CreateMemoInput
import com.assari.voicebooklm.usecase.memo.CreateMemoOutput
import com.assari.voicebooklm.usecase.memo.CreateMemoUseCase
import com.assari.voicebooklm.usecase.memo.ProcessingTime
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.server.ResponseStatusException

/**
 * VoiceController の入力検証とユースケース呼び出しを直接呼び出しでテスト。
 */
class VoiceControllerTest {

    private lateinit var createMemoUseCase: CreateMemoUseCase
    private lateinit var controller: VoiceController

    @BeforeEach
    fun setup() {
        createMemoUseCase = mockk()
        controller = VoiceController(
            createMemoUseCase = createMemoUseCase,
        )
    }

    @Test
    fun `正常にメモを生成し 201 を返す`() = runBlocking {
        val memoId = UUID.randomUUID()
        val userId = UUID.fromString("11111111-1111-1111-1111-111111111111")
        coEvery { createMemoUseCase.execute(any()) } returns stubOutput(memoId, userId)

        val file = MockMultipartFile(
            "file",
            "voice.wav",
            "audio/wav",
            byteArrayOf(1, 2, 3),
        )

        val response: ResponseEntity<VoiceMemoCreatedResponse> = controller.createMemo(
            userId = userId,
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

        val exception = assertThrows(ResponseStatusException::class.java) {
            runBlocking {
                controller.createMemo(
                    userId = null,
                    file = file,
                    language = null,
                )
            }
        }
        assertEquals(HttpStatus.UNAUTHORIZED, exception.statusCode)
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
                    userId = UUID.fromString("11111111-1111-1111-1111-111111111111"),
                    file = file,
                    language = null,
                )
            }
        }
        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
    }


    @Test
    fun `WAV以外の音声形式は 400`() {
        val file = MockMultipartFile(
            "file",
            "voice.mp3",
            "audio/mp3",
            byteArrayOf(1, 2, 3),
        )

        val ex = assertThrows(ResponseStatusException::class.java) {
            runBlocking {
                controller.createMemo(
                    userId = UUID.fromString("11111111-1111-1111-1111-111111111111"),
                    file = file,
                    language = null,
                )
            }
        }
        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
        assertTrue(ex.reason?.contains("Only WAV format is supported") == true)
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
                    userId = UUID.fromString("11111111-1111-1111-1111-111111111111"),
                    file = file,
                    language = null,
                )
            }
        }
        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
    }

    private fun stubOutput(memoId: UUID, userId: UUID): CreateMemoOutput {
        val voiceMemo = VoiceMemo(
            id = memoId,
            userId = userId,
            transcription = Transcription.completed(
                text = "text",
                languageCode = "ja-JP",
                fallbackUsed = false,
            ),
            formatting = Formatting.completed(
                title = "title",
                content = "content",
                tags = listOf("t1"),
                fallbackUsed = false,
            ),
        )
        return CreateMemoOutput(
            voiceMemo = voiceMemo,
            processingTime = ProcessingTime(
                transcription = 10.milliseconds,
                formatting = 20.milliseconds,
                persistence = 30.milliseconds,
                total = Duration.parse("0.06s"),
            ),
        )
    }
}
