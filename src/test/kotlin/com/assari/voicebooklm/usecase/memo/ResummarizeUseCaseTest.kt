package com.assari.voicebooklm.usecase.memo

import com.assari.voicebooklm.domain.exception.DomainException
import com.assari.voicebooklm.domain.gateway.MemoFormatCommand
import com.assari.voicebooklm.domain.gateway.MemoFormatResult
import com.assari.voicebooklm.domain.gateway.MemoFormatter
import com.assari.voicebooklm.domain.model.Folder
import com.assari.voicebooklm.domain.model.VoiceMemo
import com.assari.voicebooklm.infrastructure.service.FolderPathResolver
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.TestTimeSource
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ResummarizeUseCase の振る舞いをテストダブルで検証
 */
@OptIn(ExperimentalTime::class)
class ResummarizeUseCaseTest {

    @Test
    fun `再要約が成功する`() = runTest {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()

        // 既存のメモを作成
        val existingMemo = VoiceMemo.create(
            id = memoId,
            userId = userId,
            languageCode = "ja-JP",
        )
            .startTranscription()
            .completeTranscription(text = "元の文字起こし", fallbackUsed = false)
            .startFormatting()
            .completeFormatting(
                title = "元のタイトル",
                content = "元の内容",
                tags = listOf("old-tag"),
                fallbackUsed = false,
                folderId = null,
            )

        val voiceMemoRepository = FakeVoiceMemoRepository()
        voiceMemoRepository.save(existingMemo)

        val folderRepository = FakeFolderRepository()
        val memoFormatter = FakeMemoFormatter(
            title = "新しいタイトル",
            content = "新しい内容",
            tags = listOf("new-tag"),
            folderPath = null,
        )
        val executionTimer = FakeExecutionTimer(
            timeSource = TestTimeSource(),
            durations = listOf(150.milliseconds),
        )

        val useCase = ResummarizeUseCase(
            voiceMemoRepository = voiceMemoRepository,
            folderRepository = folderRepository,
            folderPathResolver = FolderPathResolver(folderRepository),
            memoFormatter = memoFormatter,
            executionTimer = executionTimer,
        )

        // 実行
        val result = useCase.execute(
            ResummarizeInput(
                memoId = memoId,
                userId = userId,
                editedTranscription = "編集された文字起こし",
            ),
        )

        // 検証
        assertEquals(memoId, result.voiceMemo.id)
        assertEquals("新しいタイトル", result.voiceMemo.title)
        assertEquals("新しい内容", result.voiceMemo.content)
        assertEquals(listOf("new-tag"), result.voiceMemo.tags)
        assertEquals("編集された文字起こし", result.voiceMemo.transcriptionText)
        assertEquals(150.milliseconds, result.formattingDuration)

        // MemoFormatterに渡されたコマンドを確認
        assertNotNull(memoFormatter.receivedCommand)
        assertEquals("編集された文字起こし", memoFormatter.receivedCommand?.transcript)
        assertEquals(userId, memoFormatter.receivedCommand?.userId)
    }

    @Test
    fun `他人のメモの場合は例外を返す`() = runTest {
        val userId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        val memoId = UUID.randomUUID()

        // 他人のメモを作成
        val otherUserMemo = VoiceMemo.create(
            id = memoId,
            userId = otherUserId,
            languageCode = "ja-JP",
        )
            .startTranscription()
            .completeTranscription(text = "文字起こし", fallbackUsed = false)
            .startFormatting()
            .completeFormatting(
                title = "タイトル",
                content = "内容",
                tags = emptyList(),
                fallbackUsed = false,
                folderId = null,
            )

        val voiceMemoRepository = FakeVoiceMemoRepository()
        voiceMemoRepository.save(otherUserMemo)

        val folderRepository = FakeFolderRepository()
        val useCase = ResummarizeUseCase(
            voiceMemoRepository = voiceMemoRepository,
            folderRepository = folderRepository,
            folderPathResolver = FolderPathResolver(folderRepository),
            memoFormatter = FakeMemoFormatter("", "", emptyList()),
            executionTimer = FakeExecutionTimer(TestTimeSource(), emptyList()),
        )

        // 実行して例外を確認
        assertThrowsSuspend<DomainException> {
            useCase.execute(
                ResummarizeInput(
                    memoId = memoId,
                    userId = userId,
                    editedTranscription = "編集された文字起こし",
                ),
            )
        }
    }

    @Test
    fun `削除済みメモの場合は例外を返す`() = runTest {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()

        // 削除済みメモを作成
        val deletedMemo = VoiceMemo.create(
            id = memoId,
            userId = userId,
            languageCode = "ja-JP",
        )
            .startTranscription()
            .completeTranscription(text = "文字起こし", fallbackUsed = false)
            .startFormatting()
            .completeFormatting(
                title = "タイトル",
                content = "内容",
                tags = emptyList(),
                fallbackUsed = false,
                folderId = null,
            )
            .markAsDeleted()

        val voiceMemoRepository = FakeVoiceMemoRepository()
        voiceMemoRepository.save(deletedMemo)

        val folderRepository = FakeFolderRepository()
        val useCase = ResummarizeUseCase(
            voiceMemoRepository = voiceMemoRepository,
            folderRepository = folderRepository,
            folderPathResolver = FolderPathResolver(folderRepository),
            memoFormatter = FakeMemoFormatter("", "", emptyList()),
            executionTimer = FakeExecutionTimer(TestTimeSource(), emptyList()),
        )

        // 実行して例外を確認
        assertThrowsSuspend<DomainException> {
            useCase.execute(
                ResummarizeInput(
                    memoId = memoId,
                    userId = userId,
                    editedTranscription = "編集された文字起こし",
                ),
            )
        }
    }

    @Test
    fun `メモが存在しない場合は例外を返す`() = runTest {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()

        val voiceMemoRepository = FakeVoiceMemoRepository()
        val folderRepository = FakeFolderRepository()
        val useCase = ResummarizeUseCase(
            voiceMemoRepository = voiceMemoRepository,
            folderRepository = folderRepository,
            folderPathResolver = FolderPathResolver(folderRepository),
            memoFormatter = FakeMemoFormatter("", "", emptyList()),
            executionTimer = FakeExecutionTimer(TestTimeSource(), emptyList()),
        )

        // 実行して例外を確認
        assertThrowsSuspend<DomainException> {
            useCase.execute(
                ResummarizeInput(
                    memoId = memoId,
                    userId = userId,
                    editedTranscription = "編集された文字起こし",
                ),
            )
        }
    }

    @Test
    fun `空の文字起こしテキストの場合は例外を返す`() = runTest {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()

        val existingMemo = VoiceMemo.create(
            id = memoId,
            userId = userId,
            languageCode = "ja-JP",
        )
            .startTranscription()
            .completeTranscription(text = "元の文字起こし", fallbackUsed = false)
            .startFormatting()
            .completeFormatting(
                title = "タイトル",
                content = "内容",
                tags = emptyList(),
                fallbackUsed = false,
                folderId = null,
            )

        val voiceMemoRepository = FakeVoiceMemoRepository()
        voiceMemoRepository.save(existingMemo)

        val folderRepository = FakeFolderRepository()
        val useCase = ResummarizeUseCase(
            voiceMemoRepository = voiceMemoRepository,
            folderRepository = folderRepository,
            folderPathResolver = FolderPathResolver(folderRepository),
            memoFormatter = FakeMemoFormatter("", "", emptyList()),
            executionTimer = FakeExecutionTimer(TestTimeSource(), emptyList()),
        )

        // 実行して例外を確認
        assertThrowsSuspend<DomainException> {
            useCase.execute(
                ResummarizeInput(
                    memoId = memoId,
                    userId = userId,
                    editedTranscription = "",
                ),
            )
        }
    }

    @Test
    fun `AI整形が失敗した場合はフォールバックを使用する`() = runTest {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()

        val existingMemo = VoiceMemo.create(
            id = memoId,
            userId = userId,
            languageCode = "ja-JP",
        )
            .startTranscription()
            .completeTranscription(text = "元の文字起こし", fallbackUsed = false)
            .startFormatting()
            .completeFormatting(
                title = "元のタイトル",
                content = "元の内容",
                tags = listOf("old-tag"),
                fallbackUsed = false,
                folderId = null,
            )

        val voiceMemoRepository = FakeVoiceMemoRepository()
        voiceMemoRepository.save(existingMemo)

        val folderRepository = FakeFolderRepository()
        val memoFormatter = FailingMemoFormatter()
        val executionTimer = FakeExecutionTimer(TestTimeSource(), listOf(100.milliseconds))

        val useCase = ResummarizeUseCase(
            voiceMemoRepository = voiceMemoRepository,
            folderRepository = folderRepository,
            folderPathResolver = FolderPathResolver(folderRepository),
            memoFormatter = memoFormatter,
            executionTimer = executionTimer,
        )

        // 実行
        val result = useCase.execute(
            ResummarizeInput(
                memoId = memoId,
                userId = userId,
                editedTranscription = "編集された文字起こし",
            ),
        )

        // フォールバック結果を検証
        assertEquals("ボイスメモ", result.voiceMemo.title)
        assertEquals("編集された文字起こし", result.voiceMemo.content)
        assertEquals(emptyList<String>(), result.voiceMemo.tags)
        assertEquals("編集された文字起こし", result.voiceMemo.transcriptionText)
        assertTrue(result.voiceMemo.formatting.fallbackUsed)
    }

    @Test
    fun `フォルダーパスが指定された場合はフォルダーを作成する`() = runTest {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()

        val existingMemo = VoiceMemo.create(
            id = memoId,
            userId = userId,
            languageCode = "ja-JP",
        )
            .startTranscription()
            .completeTranscription(text = "元の文字起こし", fallbackUsed = false)
            .startFormatting()
            .completeFormatting(
                title = "元のタイトル",
                content = "元の内容",
                tags = emptyList(),
                fallbackUsed = false,
                folderId = null,
            )

        val voiceMemoRepository = FakeVoiceMemoRepository()
        voiceMemoRepository.save(existingMemo)

        val folderRepository = FakeFolderRepository()
        val memoFormatter = FakeMemoFormatter(
            title = "新しいタイトル",
            content = "新しい内容",
            tags = emptyList(),
            folderPath = "仕事/議事録",
        )
        val executionTimer = FakeExecutionTimer(TestTimeSource(), listOf(150.milliseconds))

        val useCase = ResummarizeUseCase(
            voiceMemoRepository = voiceMemoRepository,
            folderRepository = folderRepository,
            folderPathResolver = FolderPathResolver(folderRepository),
            memoFormatter = memoFormatter,
            executionTimer = executionTimer,
        )

        // 実行
        val result = useCase.execute(
            ResummarizeInput(
                memoId = memoId,
                userId = userId,
                editedTranscription = "編集された文字起こし",
            ),
        )

        // フォルダーが作成されたことを確認
        assertNotNull(result.voiceMemo.formatting.folderId)
        val createdFolders = folderRepository.savedFolders
        assertTrue(createdFolders.size >= 2) // "仕事" と "議事録" の2つ

        val workFolder = createdFolders.find { it.name == "仕事" && it.parentId == null }
        assertNotNull(workFolder)

        val meetingFolder = createdFolders.find { it.name == "議事録" && it.parentId == workFolder?.id }
        assertNotNull(meetingFolder)

        assertEquals(meetingFolder?.id, result.voiceMemo.formatting.folderId)
    }

    @Test
    fun `既存フォルダーパスがAI整形に渡される`() = runTest {
        val userId = UUID.randomUUID()
        val memoId = UUID.randomUUID()

        // 既存フォルダーを作成
        val folderRepository = FakeFolderRepository()
        val folder1 = Folder.create(
            id = UUID.randomUUID(),
            userId = userId,
            name = "既存フォルダー1",
            parentId = null,
        )
        val folder2 = Folder.create(
            id = UUID.randomUUID(),
            userId = userId,
            name = "既存フォルダー2",
            parentId = null,
        )
        folderRepository.save(folder1)
        folderRepository.save(folder2)

        val existingMemo = VoiceMemo.create(
            id = memoId,
            userId = userId,
            languageCode = "ja-JP",
        )
            .startTranscription()
            .completeTranscription(text = "元の文字起こし", fallbackUsed = false)
            .startFormatting()
            .completeFormatting(
                title = "元のタイトル",
                content = "元の内容",
                tags = emptyList(),
                fallbackUsed = false,
                folderId = null,
            )

        val voiceMemoRepository = FakeVoiceMemoRepository()
        voiceMemoRepository.save(existingMemo)

        val memoFormatter = FakeMemoFormatter(
            title = "新しいタイトル",
            content = "新しい内容",
            tags = emptyList(),
            folderPath = null,
        )
        val executionTimer = FakeExecutionTimer(TestTimeSource(), listOf(150.milliseconds))

        val useCase = ResummarizeUseCase(
            voiceMemoRepository = voiceMemoRepository,
            folderRepository = folderRepository,
            folderPathResolver = FolderPathResolver(folderRepository),
            memoFormatter = memoFormatter,
            executionTimer = executionTimer,
        )

        // 実行
        useCase.execute(
            ResummarizeInput(
                memoId = memoId,
                userId = userId,
                editedTranscription = "編集された文字起こし",
            ),
        )

        // MemoFormatterに渡されたexistingFolderPathsを確認
        assertNotNull(memoFormatter.receivedCommand)
        val existingPaths = memoFormatter.receivedCommand?.existingFolderPaths ?: emptyList()
        assertEquals(2, existingPaths.size)
        assertTrue(existingPaths.contains("既存フォルダー1"))
        assertTrue(existingPaths.contains("既存フォルダー2"))
    }
}

/**
 * 常に失敗するMemoFormatterモック（フォールバックテスト用）
 */
private class FailingMemoFormatter : MemoFormatter {
    override suspend fun format(command: MemoFormatCommand): MemoFormatResult {
        throw RuntimeException("AI formatting failed")
    }
}
