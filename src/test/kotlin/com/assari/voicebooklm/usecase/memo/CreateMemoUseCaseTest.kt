package com.assari.voicebooklm.usecase.memo

import com.assari.voicebooklm.domain.gateway.MemoFormatCommand
import com.assari.voicebooklm.domain.gateway.MemoFormatResult
import com.assari.voicebooklm.domain.gateway.MemoFormatter
import com.assari.voicebooklm.domain.gateway.SpeechTranscriber
import com.assari.voicebooklm.domain.gateway.SpeechTranscriptionCommand
import com.assari.voicebooklm.domain.gateway.SpeechTranscriptionResult
import com.assari.voicebooklm.domain.model.Folder
import com.assari.voicebooklm.domain.model.Tag
import com.assari.voicebooklm.domain.model.VoiceMemo
import com.assari.voicebooklm.domain.repository.FolderRepository
import com.assari.voicebooklm.domain.repository.TagRepository
import com.assari.voicebooklm.domain.repository.TagWithCount
import com.assari.voicebooklm.domain.repository.VoiceMemoRepository
import com.assari.voicebooklm.infrastructure.service.FolderPathResolver
import com.assari.voicebooklm.usecase.support.ExecutionTimer
import com.assari.voicebooklm.usecase.support.TimedResult
import com.github.f4b6a3.uuid.UuidCreator
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.TestTimeSource
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * CreateMemoUseCase の振る舞いをテストダブルで検証。
 */
@OptIn(ExperimentalTime::class)
class CreateMemoUseCaseTest {

    @Test
    fun `文字起こしから保存までを計測しつつ実行する`() = runTest {
        val userId = UUID.randomUUID()
        val transcribedText = "議事録 メモ"
        val timeSource = TestTimeSource()

        val speechTranscriber = FakeSpeechTranscriber(transcribedText)
        val memoFormatter = FakeMemoFormatter(
            title = "AI が作ったタイトル",
            content = "整形済みの本文",
            tags = listOf(" voice ", " memo"),
        )
        val voiceMemoRepository = FakeVoiceMemoRepository()
        val folderRepository = FakeFolderRepository()
        val tagRepository = FakeTagRepository()
        val folderPathResolver = FolderPathResolver(folderRepository)
        val executionTimer = FakeExecutionTimer(
            timeSource = timeSource,
            durations = listOf(100.milliseconds, 200.milliseconds, 50.milliseconds),
        )

        val useCase = CreateMemoUseCase(
            voiceMemoRepository = voiceMemoRepository,
            speechTranscriber = speechTranscriber,
            memoFormatter = memoFormatter,
            folderRepository = folderRepository,
            folderPathResolver = folderPathResolver,
            tagRepository = tagRepository,
            executionTimer = executionTimer,
            timeSource = timeSource,
        )

        val result = useCase.execute(
            CreateMemoInput(
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
        assertEquals(transcribedText, memoFormatter.receivedCommand?.transcript)

        val savedMemo = voiceMemoRepository.savedMemos.single()
        assertEquals(userId, savedMemo.userId)
        assertEquals("AI が作ったタイトル", savedMemo.title)
        assertEquals("整形済みの本文", savedMemo.content)
        // タグがマスタに登録され、tagIdsとして保存されている
        assertEquals(2, savedMemo.tagIds.size)
        // 登録されたタグ名を検証
        val savedTagNames = tagRepository.savedTags.map { it.name }
        assertTrue(savedTagNames.contains("voice"))
        assertTrue(savedTagNames.contains("memo"))

        assertEquals(100.milliseconds, result.processingTime.transcription)
        assertEquals(200.milliseconds, result.processingTime.formatting)
        assertEquals(50.milliseconds, result.processingTime.persistence)
        assertEquals(350.milliseconds, result.processingTime.total)
        assertEquals(false, result.voiceMemo.transcription.fallbackUsed)
        assertEquals(false, result.voiceMemo.formatting.fallbackUsed)
    }

    @Test
    fun `処理全体が30秒未満で完了する`() = runTest {
        val userId = UUID.randomUUID()
        val timeSource = TestTimeSource()
        val folderRepository = FakeFolderRepository()
        val tagRepository = FakeTagRepository()

        val useCase = CreateMemoUseCase(
            voiceMemoRepository = FakeVoiceMemoRepository(),
            speechTranscriber = FakeSpeechTranscriber("text"),
            memoFormatter = FakeMemoFormatter(
                title = "title",
                content = "content",
                tags = emptyList(),
            ),
            folderRepository = folderRepository,
            folderPathResolver = FolderPathResolver(folderRepository),
            tagRepository = tagRepository,
            executionTimer = FakeExecutionTimer(
                timeSource = timeSource,
                durations = listOf(5.seconds, 10.seconds, 5.seconds),
            ),
            timeSource = timeSource,
        )

        val result = useCase.execute(
            CreateMemoInput(
                userId = userId,
                audio = ByteArray(1) { 1 },
                audioMimeType = "audio/wav",
                language = "ja-JP",
            ),
        )

        assertTrue(
            result.processingTime.total < 30.seconds,
            "processing should finish under 30 seconds",
        )
    }

    @Test
    fun `音声が空の場合は例外を返す`() = runTest {
        val folderRepository = FakeFolderRepository()
        val tagRepository = FakeTagRepository()
        val useCase = CreateMemoUseCase(
            voiceMemoRepository = FakeVoiceMemoRepository(),
            speechTranscriber = FakeSpeechTranscriber(""),
            memoFormatter = FakeMemoFormatter(
                title = "",
                content = "",
                tags = emptyList(),
            ),
            folderRepository = folderRepository,
            folderPathResolver = FolderPathResolver(folderRepository),
            tagRepository = tagRepository,
            executionTimer = FakeExecutionTimer(TestTimeSource(), emptyList()),
            timeSource = TestTimeSource(),
        )

        assertThrowsSuspend<IllegalArgumentException> {
            useCase.execute(
                CreateMemoInput(
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

    override suspend fun transcribe(command: SpeechTranscriptionCommand): SpeechTranscriptionResult {
        receivedCommand = command
        return SpeechTranscriptionResult(
            text = transcript,
            languageCode = command.languageCode,
        )
    }
}

private class FakeMemoFormatter(
    private val title: String,
    private val content: String,
    private val tags: List<String>,
    private val folderPath: String? = null,
) : MemoFormatter {
    var receivedCommand: MemoFormatCommand? = null

    override suspend fun format(command: MemoFormatCommand): MemoFormatResult {
        receivedCommand = command
        return MemoFormatResult(
            title = title,
            content = content,
            tags = tags,
            folderPath = folderPath,
        )
    }
}

private class FakeVoiceMemoRepository : VoiceMemoRepository {
    val savedMemos = mutableListOf<VoiceMemo>()

    override suspend fun save(voiceMemo: VoiceMemo): VoiceMemo {
        savedMemos += voiceMemo
        return voiceMemo
    }

    override suspend fun findById(id: UUID): VoiceMemo? = savedMemos.find { it.id == id }

    override suspend fun findByUserId(userId: UUID): List<VoiceMemo> = savedMemos.filter { it.userId == userId }

    override suspend fun findByUserIdWithKeyword(userId: UUID, keyword: String): List<VoiceMemo> =
        savedMemos.filter { memo ->
            memo.userId == userId && !memo.deleted &&
                (memo.formatting.title?.contains(keyword, ignoreCase = true) == true ||
                    memo.formatting.content?.contains(keyword, ignoreCase = true) == true)
        }

    override fun deleteByUserId(userId: UUID) {
        savedMemos.removeIf { it.userId == userId }
    }

    override suspend fun existsByUserIdAndFolderId(userId: UUID, folderId: UUID): Boolean =
        savedMemos.any { it.userId == userId && it.formatting.folderId == folderId && !it.deleted }
}

private class FakeFolderRepository : FolderRepository {
    val savedFolders = mutableListOf<Folder>()

    override suspend fun save(folder: Folder): Folder {
        savedFolders.removeIf { it.id == folder.id }
        savedFolders += folder
        return folder
    }

    override suspend fun findById(id: UUID): Folder? = savedFolders.find { it.id == id }

    override suspend fun findByUserId(userId: UUID): List<Folder> = savedFolders.filter { it.userId == userId }

    override suspend fun findByUserIdAndParentId(userId: UUID, parentId: UUID?): List<Folder> =
        savedFolders.filter { it.userId == userId && it.parentId == parentId }

    override suspend fun findByUserIdAndPath(userId: UUID, path: String): Folder? = null

    override suspend fun findDescendantIds(folderId: UUID): List<UUID> = emptyList()

    override suspend fun delete(id: UUID) {
        savedFolders.removeIf { it.id == id }
    }

    override suspend fun existsByUserIdAndParentIdAndName(
        userId: UUID,
        parentId: UUID?,
        name: String,
        excludeId: UUID?,
    ): Boolean =
        savedFolders.any {
            it.userId == userId &&
                it.parentId == parentId &&
                it.name == name &&
                (excludeId == null || it.id != excludeId)
        }

    override fun deleteByUserId(userId: UUID) {
        savedFolders.removeIf { it.userId == userId }
    }
}

private class FakeTagRepository : TagRepository {
    val savedTags = mutableListOf<Tag>()

    override suspend fun save(tag: Tag): Tag {
        savedTags.removeIf { it.id == tag.id }
        savedTags += tag
        return tag
    }

    override suspend fun findById(id: UUID): Tag? = savedTags.find { it.id == id }

    override suspend fun findByUserId(userId: UUID): List<Tag> = savedTags.filter { it.userId == userId }

    override suspend fun findByUserIdAndName(userId: UUID, name: String): Tag? =
        savedTags.find { it.userId == userId && it.name == name }

    override suspend fun findByUserIdAndNames(userId: UUID, names: List<String>): List<Tag> =
        savedTags.filter { it.userId == userId && names.contains(it.name) }

    override suspend fun findTagsWithCountByUserId(userId: UUID, limit: Int?): List<TagWithCount> = emptyList()

    override suspend fun delete(id: UUID) {
        savedTags.removeIf { it.id == id }
    }

    override fun deleteByUserId(userId: UUID) {
        savedTags.removeIf { it.userId == userId }
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
