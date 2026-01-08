package com.assari.voicebooklm.presentation.controller.tag

import com.assari.voicebooklm.domain.model.Tag
import com.assari.voicebooklm.domain.repository.TagWithCount
import com.assari.voicebooklm.usecase.tag.ListTagsInput
import com.assari.voicebooklm.usecase.tag.ListTagsOutput
import com.assari.voicebooklm.usecase.tag.ListTagsUseCase
import com.github.f4b6a3.uuid.UuidCreator
import io.mockk.coEvery
import io.mockk.mockk
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

/**
 * TagController の認証判定とレスポンス形式を検証。
 */
class TagControllerTest {

    private lateinit var listTagsUseCase: ListTagsUseCase
    private lateinit var controller: TagController

    @BeforeEach
    fun setup() {
        listTagsUseCase = mockk()
        controller = TagController(listTagsUseCase = listTagsUseCase)
    }

    @Test
    fun `認証済みユーザーのタグ一覧を使用回数順で返す`() = runBlocking {
        val userId = UUID.randomUUID()
        val tags = listOf(
            TagWithCount(tag = createTag(userId, "仕事"), count = 5),
            TagWithCount(tag = createTag(userId, "アイデア"), count = 3),
            TagWithCount(tag = createTag(userId, "買い物"), count = 1),
        )
        coEvery { listTagsUseCase.execute(ListTagsInput(userId)) } returns ListTagsOutput(tags)

        val response = controller.listTags(userId)

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = requireNotNull(response.body) { "response body should not be null" }
        assertEquals(3, body.tags.size)
        assertEquals("仕事", body.tags[0].name)
        assertEquals(5, body.tags[0].count)
        assertEquals("アイデア", body.tags[1].name)
        assertEquals(3, body.tags[1].count)
        assertEquals("買い物", body.tags[2].name)
        assertEquals(1, body.tags[2].count)
    }

    @Test
    fun `タグが存在しない場合は空リストを返す`() = runBlocking {
        val userId = UUID.randomUUID()
        coEvery { listTagsUseCase.execute(ListTagsInput(userId)) } returns ListTagsOutput(emptyList())

        val response = controller.listTags(userId)

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = requireNotNull(response.body) { "response body should not be null" }
        assertTrue(body.tags.isEmpty())
    }

    @Test
    fun `未認証は401を返す`() = runBlocking {
        val response = controller.listTags(null)

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertNull(response.body)
    }

    private fun createTag(userId: UUID, name: String): Tag = Tag.create(
        id = UuidCreator.getTimeOrderedEpoch(),
        userId = userId,
        name = name,
    )
}
