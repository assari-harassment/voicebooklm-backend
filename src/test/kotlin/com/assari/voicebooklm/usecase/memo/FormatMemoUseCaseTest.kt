package com.assari.voicebooklm.usecase.memo

import com.assari.voicebooklm.domain.model.FormattingStatus
import com.assari.voicebooklm.domain.model.TranscriptionStatus
import com.assari.voicebooklm.infrastructure.service.FolderPathResolver
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.TestTimeSource
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * FormatMemoUseCase の振る舞いをテストダブルで検証。
 */
@OptIn(ExperimentalTime::class)
class FormatMemoUseCaseTest {

    @Test
    fun `文字起こしテキストをAI整形して保存する`() = runTest {
        val userId = UUID.randomUUID()
        val transcriptionText = "今日の会議の議事録です。参加者は山田と佐藤です。"
        val timeSource = TestTimeSource()

        val memoFormatter = FakeMemoFormatter(
            title = "会議議事録",
            content = "整形済みの議事録本文",
            tags = listOf("会議", "議事録"),
        )
        val voiceMemoRepository = InMemoryVoiceMemoRepository()
        val folderRepository = FakeFolderRepository()
        val folderPathResolver = FolderPathResolver(folderRepository)
        val executionTimer = FakeExecutionTimer(
            timeSource = timeSource,
            durations = listOf(200.milliseconds, 50.milliseconds),
        )

        val useCase = FormatMemoUseCase(
            voiceMemoRepository = voiceMemoRepository,
            memoFormatter = memoFormatter,
            folderRepository = folderRepository,
            folderPathResolver = folderPathResolver,
            executionTimer = executionTimer,
            timeSource = timeSource,
        )

        val result = useCase.execute(
            FormatMemoInput(
                userId = userId,
                transcription = transcriptionText,
                language = "ja-JP",
            ),
        )

        // MemoFormatter に渡されたコマンドを検証
        val receivedCommand = requireNotNull(memoFormatter.receivedCommand)
        assertEquals(transcriptionText, receivedCommand.transcript)
        assertEquals(userId, receivedCommand.userId)

        // 保存されたメモを検証
        val savedMemo = result.voiceMemo
        assertEquals(userId, savedMemo.userId)
        assertEquals("会議議事録", savedMemo.title)
        assertEquals("整形済みの議事録本文", savedMemo.content)
        assertEquals(listOf("会議", "議事録"), savedMemo.tags)
        assertEquals(TranscriptionStatus.COMPLETED, savedMemo.transcription.status)
        assertEquals(FormattingStatus.COMPLETED, savedMemo.formatting.status)
        assertFalse(savedMemo.formatting.fallbackUsed)

        // 処理時間を検証
        assertEquals(200.milliseconds, result.processingTime.formatting)
        assertEquals(50.milliseconds, result.processingTime.persistence)
    }

    @Test
    fun `AI整形が失敗した場合はフォールバックで保存する`() = runTest {
        val userId = UUID.randomUUID()
        val transcriptionText = "これはテストです"
        val timeSource = TestTimeSource()

        // 例外をスローするFormatter
        val failingFormatter = object : com.assari.voicebooklm.domain.gateway.MemoFormatter {
            override suspend fun format(
                command: com.assari.voicebooklm.domain.gateway.MemoFormatCommand
            ): com.assari.voicebooklm.domain.gateway.MemoFormatResult {
                throw RuntimeException("AI整形に失敗しました")
            }
        }

        val voiceMemoRepository = InMemoryVoiceMemoRepository()
        val folderRepository = FakeFolderRepository()
        val folderPathResolver = FolderPathResolver(folderRepository)
        val executionTimer = FakeExecutionTimer(
            timeSource = timeSource,
            durations = listOf(100.milliseconds, 30.milliseconds),
        )

        val useCase = FormatMemoUseCase(
            voiceMemoRepository = voiceMemoRepository,
            memoFormatter = failingFormatter,
            folderRepository = folderRepository,
            folderPathResolver = folderPathResolver,
            executionTimer = executionTimer,
            timeSource = timeSource,
        )

        val result = useCase.execute(
            FormatMemoInput(
                userId = userId,
                transcription = transcriptionText,
            ),
        )

        // フォールバックが使用されていることを検証
        val savedMemo = result.voiceMemo
        assertEquals("ボイスメモ", savedMemo.title)
        assertEquals(transcriptionText, savedMemo.content)
        assertTrue(savedMemo.tags.isEmpty())
        assertTrue(savedMemo.formatting.fallbackUsed)
    }

    @Test
    fun `空の文字起こしテキストは例外を返す`() = runTest {
        val folderRepository = FakeFolderRepository()
        val useCase = FormatMemoUseCase(
            voiceMemoRepository = InMemoryVoiceMemoRepository(),
            memoFormatter = FakeMemoFormatter("", "", emptyList()),
            folderRepository = folderRepository,
            folderPathResolver = FolderPathResolver(folderRepository),
            executionTimer = FakeExecutionTimer(TestTimeSource(), emptyList()),
            timeSource = TestTimeSource(),
        )

        assertThrowsSuspend<IllegalArgumentException> {
            useCase.execute(
                FormatMemoInput(
                    userId = UUID.randomUUID(),
                    transcription = "",
                ),
            )
        }
    }

    @Test
    fun `空白のみの文字起こしテキストは例外を返す`() = runTest {
        val folderRepository = FakeFolderRepository()
        val useCase = FormatMemoUseCase(
            voiceMemoRepository = InMemoryVoiceMemoRepository(),
            memoFormatter = FakeMemoFormatter("", "", emptyList()),
            folderRepository = folderRepository,
            folderPathResolver = FolderPathResolver(folderRepository),
            executionTimer = FakeExecutionTimer(TestTimeSource(), emptyList()),
            timeSource = TestTimeSource(),
        )

        assertThrowsSuspend<IllegalArgumentException> {
            useCase.execute(
                FormatMemoInput(
                    userId = UUID.randomUUID(),
                    transcription = "   ",
                ),
            )
        }
    }

    @Test
    fun `言語コード未指定時はja-JPがデフォルトで使用される`() = runTest {
        val userId = UUID.randomUUID()
        val timeSource = TestTimeSource()

        val memoFormatter = FakeMemoFormatter(
            title = "タイトル",
            content = "本文",
            tags = emptyList(),
        )
        val voiceMemoRepository = InMemoryVoiceMemoRepository()
        val folderRepository = FakeFolderRepository()
        val executionTimer = FakeExecutionTimer(
            timeSource = timeSource,
            durations = listOf(100.milliseconds, 30.milliseconds),
        )

        val useCase = FormatMemoUseCase(
            voiceMemoRepository = voiceMemoRepository,
            memoFormatter = memoFormatter,
            folderRepository = folderRepository,
            folderPathResolver = FolderPathResolver(folderRepository),
            executionTimer = executionTimer,
            timeSource = timeSource,
        )

        val result = useCase.execute(
            FormatMemoInput(
                userId = userId,
                transcription = "テスト",
                language = null, // 未指定
            ),
        )

        // languageCode が ja-JP であることを検証
        assertEquals("ja-JP", result.voiceMemo.transcription.languageCode)
    }

    @Test
    fun `フォルダーパスが指定された場合はフォルダーが作成される`() = runTest {
        val userId = UUID.randomUUID()
        val timeSource = TestTimeSource()

        val memoFormatter = FakeMemoFormatter(
            title = "タイトル",
            content = "本文",
            tags = emptyList(),
            folderPath = "仕事/プロジェクトA",
        )
        val voiceMemoRepository = InMemoryVoiceMemoRepository()
        val folderRepository = FakeFolderRepository()
        val folderPathResolver = FolderPathResolver(folderRepository)
        val executionTimer = FakeExecutionTimer(
            timeSource = timeSource,
            durations = listOf(100.milliseconds, 30.milliseconds),
        )

        val useCase = FormatMemoUseCase(
            voiceMemoRepository = voiceMemoRepository,
            memoFormatter = memoFormatter,
            folderRepository = folderRepository,
            folderPathResolver = folderPathResolver,
            executionTimer = executionTimer,
            timeSource = timeSource,
        )

        val result = useCase.execute(
            FormatMemoInput(
                userId = userId,
                transcription = "テスト",
            ),
        )

        // フォルダーが作成されたことを検証
        assertEquals(2, folderRepository.savedFolders.size)
        val parentFolder = folderRepository.savedFolders.find { it.name == "仕事" }
        val childFolder = folderRepository.savedFolders.find { it.name == "プロジェクトA" }
        assertNotNull(parentFolder)
        assertNotNull(childFolder)
        assertEquals(parentFolder?.id, childFolder?.parentId)

        // メモにフォルダーIDが設定されていることを検証
        assertNotNull(result.voiceMemo.formatting.folderId)
        assertEquals(childFolder?.id, result.voiceMemo.formatting.folderId)
    }
}
