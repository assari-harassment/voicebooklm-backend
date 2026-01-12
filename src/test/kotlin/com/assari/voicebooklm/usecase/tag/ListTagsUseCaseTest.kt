package com.assari.voicebooklm.usecase.tag

import com.assari.voicebooklm.domain.repository.SortOrder
import com.assari.voicebooklm.domain.repository.TagSortField
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

@DisplayName("ListTagsUseCase")
class ListTagsUseCaseTest {

    private lateinit var tagRepository: InMemoryTagRepository
    private lateinit var useCase: ListTagsUseCase

    private val userId = UUID.randomUUID()
    private val otherUserId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        tagRepository = InMemoryTagRepository()
        useCase = ListTagsUseCase(tagRepository)
    }

    @Nested
    @DisplayName("タグ一覧取得")
    inner class ListTags {

        @Test
        fun `タグが存在しない場合、空のリストを返す`() = runBlocking {
            val result = useCase.execute(ListTagsInput(userId = userId))
            assertEquals(emptyList<String>(), result.tags)
        }

        @Test
        fun `ユーザーのタグのみを取得する`() = runBlocking {
            tagRepository.addTag(userId, "開発")
            tagRepository.addTag(userId, "コード")
            tagRepository.addTag(otherUserId, "他ユーザーのタグ")

            val result = useCase.execute(ListTagsInput(userId = userId))

            assertEquals(2, result.tags.size)
            assertEquals(listOf("コード", "開発"), result.tags) // デフォルトは名前順ASC
        }

        @Test
        fun `同じタグが複数回使用されても重複なしで返す`() = runBlocking {
            tagRepository.addTag(userId, "開発")
            tagRepository.addTag(userId, "開発")
            tagRepository.addTag(userId, "コード")

            val result = useCase.execute(ListTagsInput(userId = userId))

            assertEquals(2, result.tags.size)
            assertEquals(listOf("コード", "開発"), result.tags)
        }
    }

    @Nested
    @DisplayName("ソート")
    inner class Sorting {

        @BeforeEach
        fun setUpTags() {
            // 開発: 3回, コード: 1回, ミーティング: 2回
            repeat(3) { tagRepository.addTag(userId, "開発") }
            repeat(1) { tagRepository.addTag(userId, "コード") }
            repeat(2) { tagRepository.addTag(userId, "ミーティング") }
        }

        @Test
        fun `名前順昇順でソート`() = runBlocking {
            val result = useCase.execute(
                ListTagsInput(
                    userId = userId,
                    sort = TagSortField.NAME,
                    order = SortOrder.ASC,
                )
            )
            assertEquals(listOf("コード", "ミーティング", "開発"), result.tags)
        }

        @Test
        fun `名前順降順でソート`() = runBlocking {
            val result = useCase.execute(
                ListTagsInput(
                    userId = userId,
                    sort = TagSortField.NAME,
                    order = SortOrder.DESC,
                )
            )
            assertEquals(listOf("開発", "ミーティング", "コード"), result.tags)
        }

        @Test
        fun `使用回数順昇順でソート`() = runBlocking {
            val result = useCase.execute(
                ListTagsInput(
                    userId = userId,
                    sort = TagSortField.USAGE_COUNT,
                    order = SortOrder.ASC,
                )
            )
            assertEquals(listOf("コード", "ミーティング", "開発"), result.tags)
        }

        @Test
        fun `使用回数順降順でソート`() = runBlocking {
            val result = useCase.execute(
                ListTagsInput(
                    userId = userId,
                    sort = TagSortField.USAGE_COUNT,
                    order = SortOrder.DESC,
                )
            )
            assertEquals(listOf("開発", "ミーティング", "コード"), result.tags)
        }
    }

    @Nested
    @DisplayName("件数制限")
    inner class Limit {

        @BeforeEach
        fun setUpTags() {
            tagRepository.addTag(userId, "タグ1")
            tagRepository.addTag(userId, "タグ2")
            tagRepository.addTag(userId, "タグ3")
            tagRepository.addTag(userId, "タグ4")
            tagRepository.addTag(userId, "タグ5")
        }

        @Test
        fun `limitを指定すると件数を制限できる`() = runBlocking {
            val result = useCase.execute(
                ListTagsInput(
                    userId = userId,
                    limit = 3,
                )
            )
            assertEquals(3, result.tags.size)
        }

        @Test
        fun `limitがnullの場合は全件取得`() = runBlocking {
            val result = useCase.execute(
                ListTagsInput(
                    userId = userId,
                    limit = null,
                )
            )
            assertEquals(5, result.tags.size)
        }
    }
}
