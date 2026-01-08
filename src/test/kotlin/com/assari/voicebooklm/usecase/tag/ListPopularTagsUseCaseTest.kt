package com.assari.voicebooklm.usecase.tag

import com.assari.voicebooklm.domain.model.Tag
import com.assari.voicebooklm.domain.repository.TagRepository
import com.assari.voicebooklm.domain.repository.TagWithCount
import com.github.f4b6a3.uuid.UuidCreator
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * ListPopularTagsUseCase の振る舞いをテストダブルで検証。
 */
class ListPopularTagsUseCaseTest {

    @Test
    fun `ユーザーのタグ一覧を使用回数順で取得できる`() = runTest {
        val userId = UUID.randomUUID()
        val tags = listOf(
            TagWithCount(tag = createTag(userId, "仕事"), count = 5),
            TagWithCount(tag = createTag(userId, "プライベート"), count = 3),
            TagWithCount(tag = createTag(userId, "メモ"), count = 1),
        )
        val tagRepository = ListPopularTagsInMemoryTagRepository(
            tagsWithCountPerUser = mapOf(userId to tags),
        )
        val useCase = ListPopularTagsUseCase(tagRepository)

        val result = useCase.execute(ListPopularTagsInput(userId = userId))

        assertEquals(3, result.tags.size)
        assertEquals("仕事", result.tags[0].tag.name)
        assertEquals(5, result.tags[0].count)
        assertEquals("プライベート", result.tags[1].tag.name)
        assertEquals(3, result.tags[1].count)
        assertEquals("メモ", result.tags[2].tag.name)
        assertEquals(1, result.tags[2].count)
    }

    @Test
    fun `limitを指定すると上位N件のみ取得できる`() = runTest {
        val userId = UUID.randomUUID()
        val tags = listOf(
            TagWithCount(tag = createTag(userId, "仕事"), count = 5),
            TagWithCount(tag = createTag(userId, "プライベート"), count = 3),
            TagWithCount(tag = createTag(userId, "メモ"), count = 1),
        )
        val tagRepository = ListPopularTagsInMemoryTagRepository(
            tagsWithCountPerUser = mapOf(userId to tags),
        )
        val useCase = ListPopularTagsUseCase(tagRepository)

        val result = useCase.execute(ListPopularTagsInput(userId = userId, limit = 2))

        assertEquals(2, result.tags.size)
        assertEquals("仕事", result.tags[0].tag.name)
        assertEquals("プライベート", result.tags[1].tag.name)
    }

    @Test
    fun `タグが0件の場合は空の一覧を返す`() = runTest {
        val tagRepository = ListPopularTagsInMemoryTagRepository()
        val useCase = ListPopularTagsUseCase(tagRepository)

        val result = useCase.execute(ListPopularTagsInput(userId = UUID.randomUUID()))

        assertTrue(result.tags.isEmpty())
    }

    @Test
    fun `他のユーザーのタグは取得できない`() = runTest {
        val userId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        val tags = listOf(
            TagWithCount(tag = createTag(otherUserId, "他人のタグ"), count = 10),
        )
        val tagRepository = ListPopularTagsInMemoryTagRepository(
            tagsWithCountPerUser = mapOf(otherUserId to tags),
        )
        val useCase = ListPopularTagsUseCase(tagRepository)

        val result = useCase.execute(ListPopularTagsInput(userId = userId))

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
private class ListPopularTagsInMemoryTagRepository(
    private val tagsWithCountPerUser: Map<UUID, List<TagWithCount>> = emptyMap(),
) : TagRepository {
    override suspend fun save(tag: Tag): Tag = tag
    override suspend fun findById(id: UUID): Tag? = null
    override suspend fun findByUserId(userId: UUID): List<Tag> = emptyList()
    override suspend fun findByUserIdAndName(userId: UUID, name: String): Tag? = null
    override suspend fun findByUserIdAndNames(userId: UUID, names: List<String>): List<Tag> = emptyList()
    override suspend fun findTagsWithCountByUserId(userId: UUID, limit: Int?): List<TagWithCount> {
        val tags = tagsWithCountPerUser[userId] ?: emptyList()
        return if (limit != null) tags.take(limit) else tags
    }
    override suspend fun delete(id: UUID) {}
    override fun deleteByUserId(userId: UUID) {}
}
