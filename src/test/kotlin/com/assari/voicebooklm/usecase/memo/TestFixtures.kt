package com.assari.voicebooklm.usecase.memo

import com.assari.voicebooklm.domain.gateway.MemoFormatCommand
import com.assari.voicebooklm.domain.gateway.MemoFormatResult
import com.assari.voicebooklm.domain.gateway.MemoFormatter
import com.assari.voicebooklm.domain.model.Folder
import com.assari.voicebooklm.domain.model.VoiceMemo
import com.assari.voicebooklm.domain.repository.FolderRepository
import com.assari.voicebooklm.domain.repository.VoiceMemoRepository
import com.assari.voicebooklm.domain.model.buildPath
import com.assari.voicebooklm.usecase.support.ExecutionTimer
import com.assari.voicebooklm.usecase.support.TimedResult
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource

/**
 * インメモリで動作する VoiceMemoRepository のテストダブル
 */
internal typealias FakeVoiceMemoRepository = InMemoryVoiceMemoRepository

/**
 * インメモリで動作する FolderRepository のテストダブル
 */
internal class FakeFolderRepository(
    initialFolders: List<Folder> = emptyList(),
) : FolderRepository {
    private val folders = initialFolders.toMutableList()
    val savedFolders: List<Folder> get() = folders.toList()

    override suspend fun save(folder: Folder): Folder {
        folders.removeIf { it.id == folder.id }
        folders += folder
        return folder
    }

    override suspend fun findById(id: UUID): Folder? = folders.find { it.id == id }

    override suspend fun findByUserId(userId: UUID): List<Folder> = folders.filter { it.userId == userId }

    override suspend fun findByUserIdAndParentId(userId: UUID, parentId: UUID?): List<Folder> =
        folders.filter { it.userId == userId && it.parentId == parentId }

    override suspend fun findByUserIdAndPath(userId: UUID, path: String): Folder? {
        val userFolders = folders.filter { it.userId == userId }
        val folderMap = userFolders.associateBy { it.id }
        return userFolders.find { folder -> folder.buildPath(folderMap) == path }
    }

    override suspend fun findDescendantIds(folderId: UUID): List<UUID> {
        val result = mutableListOf<UUID>()
        val queue = ArrayDeque<UUID>()
        queue.add(folderId)

        while (queue.isNotEmpty()) {
            val currentId = queue.removeFirst()
            val children = folders.filter { it.parentId == currentId }
            for (child in children) {
                result.add(child.id)
                queue.add(child.id)
            }
        }

        return result
    }

    override suspend fun delete(id: UUID) {
        folders.removeIf { it.id == id }
    }

    override suspend fun existsByUserIdAndParentIdAndName(
        userId: UUID,
        parentId: UUID?,
        name: String,
        excludeId: UUID?,
    ): Boolean =
        folders.any {
            it.userId == userId &&
                it.parentId == parentId &&
                it.name == name &&
                (excludeId == null || it.id != excludeId)
        }

    override fun deleteByUserId(userId: UUID) {
        folders.removeIf { it.userId == userId }
    }
}

/**
 * テスト用の MemoFormatter
 */
internal class FakeMemoFormatter(
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

/**
 * テスト用の ExecutionTimer
 */
internal class FakeExecutionTimer(
    private val timeSource: TestTimeSource,
    private val durations: List<Duration>,
) : ExecutionTimer {
    private var callCount = 0

    override suspend fun <T> measure(block: suspend () -> T): TimedResult<T> {
        val duration = durations.getOrElse(callCount++) { 100.milliseconds }
        val result = block()
        return TimedResult(result, duration)
    }
}

/**
 * サスペンド関数の例外をアサートするヘルパー
 */
internal suspend inline fun <reified T : Throwable> assertThrowsSuspend(
    crossinline block: suspend () -> Unit
): T {
    try {
        block()
        throw AssertionError("Expected ${T::class.simpleName} but no exception was thrown")
    } catch (e: Throwable) {
        if (e is T) {
            return e
        }
        throw AssertionError("Expected ${T::class.simpleName} but got ${e::class.simpleName}: ${e.message}")
    }
}
