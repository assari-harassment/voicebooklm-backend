package com.assari.voicebooklm.domain.model.memo

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class MemoTests {
    @Test
    fun `create trims title and removes blank tags`() {
        val memo = Memo.create(
            title = "  Title  ",
            content = "content",
            tags = listOf(" a ", " ", ""),
            userId = UUID.randomUUID(),
        )

        assertEquals("Title", memo.title)
        assertEquals(listOf("a"), memo.tags)
        assertFalse(memo.deleted)
    }

    @Test
    fun `changeTitle updates title with validation`() {
        val memo = baseMemo()

        val updated = memo.changeTitle(" New ")

        assertEquals("New", updated.title)
    }

    @Test
    fun `changeContent updates content with validation`() {
        val memo = baseMemo()

        val updated = memo.changeContent("updated")

        assertEquals("updated", updated.content)
    }

    @Test
    fun `changeTags trims and filters`() {
        val memo = baseMemo()

        val updated = memo.changeTags(listOf(" tag1 ", " ", "tag2"))

        assertEquals(listOf("tag1", "tag2"), updated.tags)
    }

    @Test
    fun `markAsDeleted sets flag and is idempotent`() {
        val memo = baseMemo()

        val deletedOnce = memo.markAsDeleted()
        val deletedTwice = deletedOnce.markAsDeleted()

        assertTrue(deletedOnce.deleted)
        assertTrue(deletedTwice.deleted)
    }

    @Test
    fun `create fails when title is blank`() {
        assertThrows(IllegalArgumentException::class.java) {
            Memo.create(
                title = " ",
                content = "content",
                tags = emptyList(),
                userId = UUID.randomUUID(),
            )
        }
    }

    @Test
    fun `create fails when content is blank`() {
        assertThrows(IllegalArgumentException::class.java) {
            Memo.create(
                title = "title",
                content = " ",
                tags = emptyList(),
                userId = UUID.randomUUID(),
            )
        }
    }

    @Test
    fun `changeTitle fails when blank`() {
        val memo = baseMemo()

        assertThrows(IllegalArgumentException::class.java) {
            memo.changeTitle("  ")
        }
    }

    @Test
    fun `changeContent fails when blank`() {
        val memo = baseMemo()

        assertThrows(IllegalArgumentException::class.java) {
            memo.changeContent(" ")
        }
    }

    private fun baseMemo(): Memo = Memo.create(
        title = "title",
        content = "content",
        tags = listOf("tag"),
        userId = UUID.randomUUID(),
    )
}
