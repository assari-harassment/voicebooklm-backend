package com.assari.voicebooklm.usecase.tag

import com.assari.voicebooklm.domain.model.Tag
import com.assari.voicebooklm.domain.repository.SortOrder
import com.assari.voicebooklm.domain.repository.TagRepository
import com.assari.voicebooklm.domain.repository.TagSortField
import com.github.f4b6a3.uuid.UuidCreator
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * ListTagsUseCase の振る舞いをテストダブルで検証。
 */
class ListTagsUseCaseTest {

    @Test
    fun `ユーザーのタグ一覧を取得できる`() = runTest {
        val userId = UUID.randomUUID()
        val tags = listOf(
            createTag(userId, "仕事"),
            createTag(userId, "プライベート"),
            createTag(userId, "メモ"),
        )
        val tagRepository = ListTagsInMemoryTagRepository(
            tagsPerUser = mapOf(userId to tags),
        )
        val useCase = ListTagsUseCase(tagRepository)

        val result = useCase.execute(ListTagsInput(userId = userId))

        // デフォルトは名前昇順でソートされる
        assertEquals(3, result.tags.size)
        assertEquals("プライベート", result.tags[0].name)
        assertEquals("メモ", result.tags[1].name)
        assertEquals("仕事", result.tags[2].name)
    }

    @Test
    fun `タグが0件の場合は空の一覧を返す`() = runTest {
        val tagRepository = ListTagsInMemoryTagRepository()
        val useCase = ListTagsUseCase(tagRepository)

        val result = useCase.execute(ListTagsInput(userId = UUID.randomUUID()))

        assertTrue(result.tags.isEmpty())
    }

    @Test
    fun `他のユーザーのタグは取得できない`() = runTest {
        val userId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        val tags = listOf(
            createTag(otherUserId, "他人のタグ"),
        )
        val tagRepository = ListTagsInMemoryTagRepository(
            tagsPerUser = mapOf(otherUserId to tags),
        )
        val useCase = ListTagsUseCase(tagRepository)

        val result = useCase.execute(ListTagsInput(userId = userId))

        assertTrue(result.tags.isEmpty())
    }

    private fun createTag(userId: UUID, name: String): Tag = Tag.create(
        id = UuidCreator.getTimeOrderedEpoch(),
        userId = userId,
        name = name,
    )
}

/**
 * テスト用 InMemory TagRepository
 */
private class ListTagsInMemoryTagRepository(
    private val tagsPerUser: Map<UUID, List<Tag>> = emptyMap(),
) : TagRepository {
    override suspend fun save(tag: Tag): Tag = tag
    override suspend fun findById(id: UUID): Tag? = null
    override suspend fun findByUserId(userId: UUID): List<Tag> = tagsPerUser[userId] ?: emptyList()
    override suspend fun findByUserIdWithSort(
        userId: UUID,
        sortField: TagSortField,
        sortOrder: SortOrder,
        limit: Int?,
    ): List<Tag> {
        val tags = tagsPerUser[userId] ?: emptyList()
        // ソートを適用
        val sorted = when (sortField) {
            TagSortField.NAME -> when (sortOrder) {
                SortOrder.ASC -> tags.sortedBy { it.name }
                SortOrder.DESC -> tags.sortedByDescending { it.name }
            }
            TagSortField.USAGE -> tags // テスト用リポジトリではusage_countがないため、名前順で代用
        }
        return if (limit != null) sorted.take(limit) else sorted
    }
    override suspend fun findByUserIdAndName(userId: UUID, name: String): Tag? = null
    override suspend fun findByUserIdAndNames(userId: UUID, names: List<String>): List<Tag> = emptyList()
    override suspend fun findByIds(ids: List<UUID>): List<Tag> = emptyList()
    override suspend fun delete(id: UUID) {}
    override fun deleteByUserId(userId: UUID) {}
}
