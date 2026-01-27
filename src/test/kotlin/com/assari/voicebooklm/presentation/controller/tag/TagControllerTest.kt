package com.assari.voicebooklm.presentation.controller.tag

import com.assari.voicebooklm.domain.model.SortOrder
import com.assari.voicebooklm.domain.model.TagSortField
import com.assari.voicebooklm.usecase.tag.ListTagsInput
import com.assari.voicebooklm.usecase.tag.ListTagsOutput
import com.assari.voicebooklm.usecase.tag.ListTagsUseCase
import io.mockk.coEvery
import io.mockk.mockk
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

/**
 * TagController のテスト
 */
class TagControllerTest {

    private lateinit var listTagsUseCase: ListTagsUseCase
    private lateinit var controller: TagController

    @BeforeEach
    fun setup() {
        listTagsUseCase = mockk()
        controller = TagController(listTagsUseCase)
    }

    @Test
    fun `認証済みユーザーのタグ一覧を返す`() = runBlocking {
        val userId = UUID.randomUUID()
        val tags = listOf("開発", "コード", "ミーティング")
        val input = ListTagsInput(
            userId = userId,
            sort = TagSortField.NAME,
            order = SortOrder.ASC,
            limit = null,
        )
        coEvery { listTagsUseCase.execute(input) } returns ListTagsOutput(tags = tags)

        val response = controller.listTags(userId, "name", "asc", null)

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = requireNotNull(response.body) { "response body should not be null" }
        assertEquals(3, body.tags.size)
        assertEquals(listOf("開発", "コード", "ミーティング"), body.tags)
    }

    @Test
    fun `タグが存在しない場合は空リストを返す`() = runBlocking {
        val userId = UUID.randomUUID()
        val input = ListTagsInput(
            userId = userId,
            sort = TagSortField.NAME,
            order = SortOrder.ASC,
            limit = null,
        )
        coEvery { listTagsUseCase.execute(input) } returns ListTagsOutput(tags = emptyList())

        val response = controller.listTags(userId, "name", "asc", null)

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = requireNotNull(response.body) { "response body should not be null" }
        assertTrue(body.tags.isEmpty())
    }

    @Test
    fun `未認証の場合はResponseStatusExceptionがスローされる`() = runBlocking {
        val exception = assertThrows<ResponseStatusException> {
            controller.listTags(null, "name", "asc", null)
        }

        assertEquals(HttpStatus.UNAUTHORIZED, exception.statusCode)
        assertEquals("認証が必要です", exception.reason)
    }

    @Test
    fun `limitが0の場合はBAD_REQUESTがスローされる`() = runBlocking {
        val userId = UUID.randomUUID()
        val exception = assertThrows<ResponseStatusException> {
            controller.listTags(userId, "name", "asc", 0)
        }

        assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
        assertEquals("limitは1以上の値を指定してください", exception.reason)
    }

    @Test
    fun `limitが負の値の場合はBAD_REQUESTがスローされる`() = runBlocking {
        val userId = UUID.randomUUID()
        val exception = assertThrows<ResponseStatusException> {
            controller.listTags(userId, "name", "asc", -1)
        }

        assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
        assertEquals("limitは1以上の値を指定してください", exception.reason)
    }

    @Test
    fun `limitを指定してタグ一覧を取得できる`() = runBlocking {
        val userId = UUID.randomUUID()
        val tags = listOf("人気タグ1", "人気タグ2")
        val input = ListTagsInput(
            userId = userId,
            sort = TagSortField.USAGE_COUNT,
            order = SortOrder.DESC,
            limit = 2,
        )
        coEvery { listTagsUseCase.execute(input) } returns ListTagsOutput(tags = tags)

        val response = controller.listTags(userId, "usage_count", "desc", 2)

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = requireNotNull(response.body) { "response body should not be null" }
        assertEquals(2, body.tags.size)
        assertEquals(listOf("人気タグ1", "人気タグ2"), body.tags)
    }

    @Test
    fun `sortパラメータがusage_countの場合USAGE_COUNTでソートされる`() = runBlocking {
        val userId = UUID.randomUUID()
        val input = ListTagsInput(
            userId = userId,
            sort = TagSortField.USAGE_COUNT,
            order = SortOrder.ASC,
            limit = null,
        )
        coEvery { listTagsUseCase.execute(input) } returns ListTagsOutput(tags = listOf("tag1"))

        val response = controller.listTags(userId, "usage_count", "asc", null)

        assertEquals(HttpStatus.OK, response.statusCode)
    }

    @Test
    fun `sortパラメータが不正な値の場合NAMEでソートされる`() = runBlocking {
        val userId = UUID.randomUUID()
        val input = ListTagsInput(
            userId = userId,
            sort = TagSortField.NAME,
            order = SortOrder.ASC,
            limit = null,
        )
        coEvery { listTagsUseCase.execute(input) } returns ListTagsOutput(tags = listOf("tag1"))

        val response = controller.listTags(userId, "invalid_sort", "asc", null)

        assertEquals(HttpStatus.OK, response.statusCode)
    }

    @Test
    fun `orderパラメータがdescの場合DESCでソートされる`() = runBlocking {
        val userId = UUID.randomUUID()
        val input = ListTagsInput(
            userId = userId,
            sort = TagSortField.NAME,
            order = SortOrder.DESC,
            limit = null,
        )
        coEvery { listTagsUseCase.execute(input) } returns ListTagsOutput(tags = listOf("tag1"))

        val response = controller.listTags(userId, "name", "desc", null)

        assertEquals(HttpStatus.OK, response.statusCode)
    }

    @Test
    fun `orderパラメータが不正な値の場合ASCでソートされる`() = runBlocking {
        val userId = UUID.randomUUID()
        val input = ListTagsInput(
            userId = userId,
            sort = TagSortField.NAME,
            order = SortOrder.ASC,
            limit = null,
        )
        coEvery { listTagsUseCase.execute(input) } returns ListTagsOutput(tags = listOf("tag1"))

        val response = controller.listTags(userId, "name", "invalid_order", null)

        assertEquals(HttpStatus.OK, response.statusCode)
    }

    @Test
    fun `sortとorderパラメータは大文字小文字を区別しない`() = runBlocking {
        val userId = UUID.randomUUID()
        val input = ListTagsInput(
            userId = userId,
            sort = TagSortField.USAGE_COUNT,
            order = SortOrder.DESC,
            limit = null,
        )
        coEvery { listTagsUseCase.execute(input) } returns ListTagsOutput(tags = listOf("tag1"))

        val response = controller.listTags(userId, "USAGE_COUNT", "DESC", null)

        assertEquals(HttpStatus.OK, response.statusCode)
    }
}
