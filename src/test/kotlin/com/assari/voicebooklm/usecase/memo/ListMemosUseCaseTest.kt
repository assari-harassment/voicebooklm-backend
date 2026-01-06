package com.assari.voicebooklm.usecase.memo

import com.assari.voicebooklm.domain.model.Folder
import com.assari.voicebooklm.domain.model.VoiceMemo
import com.assari.voicebooklm.domain.repository.FolderRepository
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ListMemosUseCase の振る舞いをテストダブルで検証。
 */
class ListMemosUseCaseTest {

    @Test
    fun `ユーザーのメモ一覧を取得できる`() = runTest {
        val userId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        val memo1 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
        val memo2 = VoiceMemo.create(id = UUID.randomUUID(), userId = userId)
        val otherMemo = VoiceMemo.create(id = UUID.randomUUID(), userId = otherUserId)

        val voiceMemoRepository = InMemoryVoiceMemoRepository(
            initialMemos = listOf(memo1, memo2, otherMemo),
        )
        val folderRepository = InMemoryFolderRepository()
        val useCase = ListMemosUseCase(voiceMemoRepository, folderRepository)

        val result = useCase.execute(ListMemosInput(userId = userId))

        assertEquals(2, result.memos.size)
        assertEquals(memo1.id, result.memos[0].memo.id)
        assertEquals(memo2.id, result.memos[1].memo.id)
    }

    @Test
    fun `メモが0件の場合は空の一覧を返す`() = runTest {
        val folderRepository = InMemoryFolderRepository()
        val useCase = ListMemosUseCase(InMemoryVoiceMemoRepository(), folderRepository)

        val result = useCase.execute(ListMemosInput(userId = UUID.randomUUID()))

        assertTrue(result.memos.isEmpty())
    }
}

// インメモリで動作する FolderRepository のテストダブル。
private class InMemoryFolderRepository(
    initialFolders: List<Folder> = emptyList(),
) : FolderRepository {
    private val folders = initialFolders.toMutableList()

    override suspend fun save(folder: Folder): Folder {
        folders.removeIf { it.id == folder.id }
        folders += folder
        return folder
    }

    override suspend fun findById(id: UUID): Folder? = folders.find { it.id == id }

    override suspend fun findByUserId(userId: UUID): List<Folder> = folders.filter { it.userId == userId }

    override suspend fun findByUserIdAndParentId(userId: UUID, parentId: UUID?): List<Folder> =
        folders.filter { it.userId == userId && it.parentId == parentId }

    override suspend fun findByUserIdAndPath(userId: UUID, path: String): Folder? = null

    override suspend fun findDescendantIds(folderId: UUID): List<UUID> = emptyList()

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
