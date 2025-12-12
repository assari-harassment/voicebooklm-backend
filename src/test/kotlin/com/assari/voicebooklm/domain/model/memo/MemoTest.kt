package com.assari.voicebooklm.domain.model.memo

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class MemoTest {

    private val userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
    private val memoId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")

    @Test
    fun `create trims title and removes empty tags`() {
        val memo = Memo.create(
            title = "  Meeting notes  ",
            content = "Discuss roadmap",
            tags = listOf(" work ", " ", "planning"),
            userId = userId,
            id = memoId,
        )

        assertEquals("Meeting notes", memo.title)
        assertEquals(listOf("work", "planning"), memo.tags)
        assertFalse(memo.deleted)
    }

    @Test
    fun `changeTitle updates title and keeps others`() {
        val original = Memo.create(
            title = "Old",
            content = "Body",
            tags = listOf("tag1"),
            userId = userId,
            id = memoId,
        )

        val updated = original.changeTitle("New Title")

        assertEquals("New Title", updated.title)
        assertEquals(original.content, updated.content)
        assertEquals(original.tags, updated.tags)
        assertEquals(original.userId, updated.userId)
    }

    @Test
    fun `changeContent updates content`() {
        val original = Memo.create(
            title = "Title",
            content = "Old content",
            tags = emptyList(),
            userId = userId,
            id = memoId,
        )

        val updated = original.changeContent("New content")

        assertEquals("New content", updated.content)
        assertEquals(original.title, updated.title)
    }

    @Test
    fun `changeTags sanitizes tags and leaves memo immutable`() {
        val original = Memo.create(
            title = "Title",
            content = "Content",
            tags = listOf("a"),
            userId = userId,
            id = memoId,
        )

        val updated = original.changeTags(listOf("  a ", "", "b"))

        assertEquals(listOf("a", "b"), updated.tags)
        assertEquals(listOf("a"), original.tags)
    }

    @Test
    fun `markAsDeleted toggles flag without changing identity`() {
        val original = Memo.create(
            title = "Title",
            content = "Content",
            tags = listOf("tag"),
            userId = userId,
            id = memoId,
        )

        val deleted = original.markAsDeleted()

        assertTrue(deleted.deleted)
        assertEquals(original.id, deleted.id)
        assertEquals(original.title, deleted.title)
    }

    @Test
    fun `blank title or content is rejected`() {
        assertThrows<IllegalArgumentException> {
            Memo.create(
                title = "  ",
                content = "Body",
                tags = emptyList(),
                userId = userId,
                id = memoId,
            )
        }

        assertThrows<IllegalArgumentException> {
            Memo.create(
                title = "Title",
                content = "",
                tags = emptyList(),
                userId = userId,
                id = memoId,
            )
        }
    }
}
