package com.assari.voicebooklm.domain.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.UUID

class TagTest {

    @Test
    fun `should create tag with valid data`() {
        val id = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val name = "仕事"

        val tag = Tag.create(
            id = id,
            userId = userId,
            name = name,
        )

        assertEquals(id, tag.id)
        assertEquals(userId, tag.userId)
        assertEquals(name, tag.name)
    }

    @Test
    fun `should trim whitespace from name when creating`() {
        val id = UUID.randomUUID()
        val userId = UUID.randomUUID()

        val tag = Tag.create(
            id = id,
            userId = userId,
            name = "  仕事  ",
        )

        assertEquals("仕事", tag.name)
    }

    @Test
    fun `should restore tag with all fields`() {
        val id = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val name = "プライベート"
        val createdAt = Instant.parse("2024-01-01T00:00:00Z")
        val updatedAt = Instant.parse("2024-06-01T12:00:00Z")

        val tag = Tag.restore(
            id = id,
            userId = userId,
            name = name,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

        assertEquals(id, tag.id)
        assertEquals(userId, tag.userId)
        assertEquals(name, tag.name)
        assertEquals(createdAt, tag.createdAt)
        assertEquals(updatedAt, tag.updatedAt)
    }

    @Test
    fun `should rename and return new instance`() {
        val now = Instant.now()
        val tag = Tag(
            id = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            name = "旧名",
            createdAt = now,
            updatedAt = now,
        )

        val renamedTag = tag.rename("新名")

        // 元のインスタンスは変更されない（イミュータブル）
        assertEquals("旧名", tag.name)
        assertEquals(now, tag.updatedAt)
        // 新しいインスタンスが返される
        assertEquals("新名", renamedTag.name)
        assertTrue(renamedTag.updatedAt.isAfter(now) || renamedTag.updatedAt == now)
        // ID は同じ
        assertEquals(tag.id, renamedTag.id)
    }

    @Test
    fun `should trim whitespace from name when renaming`() {
        val tag = Tag.create(
            id = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            name = "旧名",
        )

        val renamedTag = tag.rename("  新名  ")

        assertEquals("新名", renamedTag.name)
    }

    @Test
    fun `should throw exception when name is blank`() {
        val exception = assertThrows<IllegalArgumentException> {
            Tag.create(
                id = UUID.randomUUID(),
                userId = UUID.randomUUID(),
                name = "   ",
            )
        }

        assertEquals("Tag name must not be blank", exception.message)
    }

    @Test
    fun `should throw exception when name is empty`() {
        val exception = assertThrows<IllegalArgumentException> {
            Tag.create(
                id = UUID.randomUUID(),
                userId = UUID.randomUUID(),
                name = "",
            )
        }

        assertEquals("Tag name must not be blank", exception.message)
    }

    @Test
    fun `should throw exception when name exceeds max length`() {
        val longName = "a".repeat(Tag.MAX_NAME_LENGTH + 1)

        val exception = assertThrows<IllegalArgumentException> {
            Tag.create(
                id = UUID.randomUUID(),
                userId = UUID.randomUUID(),
                name = longName,
            )
        }

        assertEquals("Tag name must not exceed ${Tag.MAX_NAME_LENGTH} characters", exception.message)
    }

    @Test
    fun `should accept name at max length boundary`() {
        val maxLengthName = "a".repeat(Tag.MAX_NAME_LENGTH)

        val tag = Tag.create(
            id = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            name = maxLengthName,
        )

        assertEquals(maxLengthName, tag.name)
    }

    @Test
    fun `should have equals based on id`() {
        val id = UUID.randomUUID()
        val now = Instant.now()

        val tag1 = Tag(
            id = id,
            userId = UUID.randomUUID(),
            name = "タグ1",
            createdAt = now,
            updatedAt = now,
        )

        val tag2 = Tag(
            id = id,
            userId = UUID.randomUUID(),
            name = "タグ2",
            createdAt = now,
            updatedAt = now,
        )

        assertEquals(tag1, tag2)
    }

    @Test
    fun `should have different equals for different ids`() {
        val now = Instant.now()

        val tag1 = Tag(
            id = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            name = "同じ名前",
            createdAt = now,
            updatedAt = now,
        )

        val tag2 = Tag(
            id = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            name = "同じ名前",
            createdAt = now,
            updatedAt = now,
        )

        assertNotEquals(tag1, tag2)
    }

    @Test
    fun `should have consistent hashCode based on id`() {
        val id = UUID.randomUUID()
        val now = Instant.now()

        val tag1 = Tag(
            id = id,
            userId = UUID.randomUUID(),
            name = "タグ1",
            createdAt = now,
            updatedAt = now,
        )

        val tag2 = Tag(
            id = id,
            userId = UUID.randomUUID(),
            name = "タグ2",
            createdAt = now,
            updatedAt = now,
        )

        assertEquals(tag1.hashCode(), tag2.hashCode())
    }
}
