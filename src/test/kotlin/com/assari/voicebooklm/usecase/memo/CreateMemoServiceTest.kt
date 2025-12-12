package com.assari.voicebooklm.usecase.memo

import com.assari.voicebooklm.domain.model.Memo
import com.assari.voicebooklm.domain.repository.MemoRepository
import com.assari.voicebooklm.usecase.memo.client.AiMemoDraft
import com.assari.voicebooklm.usecase.memo.client.AiMemoFormatCommand
import com.assari.voicebooklm.usecase.memo.client.AiMemoFormatter
import com.assari.voicebooklm.usecase.memo.client.SpeechTranscription
import com.assari.voicebooklm.usecase.memo.client.SpeechTranscriptionCommand
import com.assari.voicebooklm.usecase.memo.client.SpeechTranscriber
import com.assari.voicebooklm.usecase.support.ExecutionTimer
import com.assari.voicebooklm.usecase.support.TimedResult
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.TestTimeSource
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

/**
 * CreateMemoUseCase の振る舞いをテストダブルで検証。
 */
@OptIn(ExperimentalTime::class)
class CreateMemoServiceTest {

    @Test
    fun `文字起こしから保存までを計測しつつ実行する`() = runTest {
        val userId = UUID.randomUUID()
        val transcribedText = "議事録 メモ"
        val timeSource = TestTimeSource()

        val speechTranscriber = FakeSpeechTranscriber(transcribedText)
        val aiMemoFormatter = FakeAiMemoFormatter(
            title = "AI が作ったタイトル",
            content = "整形済みの本文",
            tags = listOf(" voice ", " memo"),
        )
        val memoRepository = FakeMemoRepository()
        val executionTimer = FakeExecutionTimer(
            timeSource = timeSource,
            durations = listOf(100.milliseconds, 200.milliseconds, 50.milliseconds),
        )

        val useCase = CreateMemoService(
            memoRepository = memoRepository,
            speechTranscriber = speechTranscriber,
            aiMemoFormatter = aiMemoFormatter,
            executionTimer = executionTimer,
            timeSource = timeSource,
        )

        val result = useCase.execute(
            CreateMemoCommand(
                userId = userId,
                audio = ByteArray(4) { 1 },
                audioMimeType = "audio/wav",
                language = "ja-JP",
            ),
        )

        val sentTranscriptionCommand = requireNotNull(speechTranscriber.receivedCommand) {
            "SpeechTranscriber should receive a command"
        }
        assertEquals("audio/wav", sentTranscriptionCommand.mimeType)
        assertEquals("ja-JP", sentTranscriptionCommand.languageCode)
        assertEquals(transcribedText, aiMemoFormatter.receivedCommand?.transcript)

        val savedMemo = memoRepository.savedMemos.single()
        assertEquals(userId, savedMemo.userId)
        assertEquals("AI が作ったタイトル", savedMemo.title)
        assertEquals("整形済みの本文", savedMemo.content)
        assertEquals(listOf("voice", "memo"), savedMemo.tags)

        assertEquals(100.milliseconds, result.processingTime.transcription)
        assertEquals(200.milliseconds, result.processingTime.formatting)
        assertEquals(50.milliseconds, result.processingTime.persistence)
        assertEquals(350.milliseconds, result.processingTime.total)
        assertEquals(false, result.fallbackUsage.transcription)
        assertEquals(false, result.fallbackUsage.formatting)
    }

    @Test
    fun `音声が空の場合は例外を返す`() = runTest {
        val useCase = CreateMemoService(
            memoRepository = FakeMemoRepository(),
            speechTranscriber = FakeSpeechTranscriber(""),
            aiMemoFormatter = FakeAiMemoFormatter(
                title = "",
                content = "",
                tags = emptyList(),
            ),
            executionTimer = FakeExecutionTimer(TestTimeSource(), emptyList()),
            timeSource = TestTimeSource(),
        )

        assertThrowsSuspend<IllegalArgumentException> {
            useCase.execute(
                CreateMemoCommand(
                    userId = UUID.randomUUID(),
                    audio = ByteArray(0),
                    audioMimeType = "audio/wav",
                    language = null,
                ),
            )
        }
    }
}

private class FakeSpeechTranscriber(
    private val transcript: String,
) : SpeechTranscriber {
    var receivedCommand: SpeechTranscriptionCommand? = null

    override suspend fun transcribe(command: SpeechTranscriptionCommand): SpeechTranscription {
        receivedCommand = command
        return SpeechTranscription(
            text = transcript,
            languageCode = command.languageCode,
        )
    }
}

private class FakeAiMemoFormatter(
    private val title: String,
    private val content: String,
    private val tags: List<String>,
) : AiMemoFormatter {
    var receivedCommand: AiMemoFormatCommand? = null

    override suspend fun format(command: AiMemoFormatCommand): AiMemoDraft {
        receivedCommand = command
        return AiMemoDraft(
            title = title,
            content = content,
            tags = tags,
        )
    }
}

private class FakeMemoRepository : MemoRepository {
    val savedMemos = mutableListOf<Memo>()

    override suspend fun save(memo: Memo): Memo {
        savedMemos += memo
        return memo
    }

    override suspend fun findById(id: UUID): Memo? = savedMemos.find { it.id == id }

    override suspend fun findByUserId(userId: UUID): List<Memo> = savedMemos.filter { it.userId == userId }

    override fun deleteByUserId(userId: UUID) {
        savedMemos.removeIf { it.userId == userId }
    }
}

private class FakeExecutionTimer(
    private val timeSource: TestTimeSource,
    durations: List<Duration>,
) : ExecutionTimer {
    private val durationQueue = ArrayDeque(durations)

    override suspend fun <T> measure(block: suspend () -> T): TimedResult<T> {
        val value = block()
        val duration = durationQueue.removeFirstOrNull() ?: Duration.ZERO
        timeSource += duration
        return TimedResult(value, duration)
    }
}

private suspend inline fun <reified T : Throwable> assertThrowsSuspend(
    noinline block: suspend () -> Unit,
): T {
    try {
        block()
    } catch (ex: Throwable) {
        if (ex is T) {
            return ex
        }
        throw ex
    }
    throw AssertionError("Expected exception ${T::class.simpleName} was not thrown")
}
