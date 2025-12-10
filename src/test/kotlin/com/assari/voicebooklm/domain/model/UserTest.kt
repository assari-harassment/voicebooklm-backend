package com.assari.voicebooklm.domain.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class UserTest {

    @Test
    fun `should create user with valid data`() {
        val id = UUID.randomUUID()
        val googleSub = "google-sub-12345"
        val email = "test@example.com"
        val name = "Test User"
        val now = Instant.now()

        val user = User(
            id = id,
            googleSub = googleSub,
            email = email,
            name = name,
            createdAt = now,
            updatedAt = now
        )

        assertEquals(id, user.id)
        assertEquals(googleSub, user.googleSub)
        assertEquals(email, user.email)
        assertEquals(name, user.name)
        assertEquals(now, user.createdAt)
        assertEquals(now, user.updatedAt)
    }

    @Test
    fun `should have equals based on id`() {
        val id = UUID.randomUUID()
        val now = Instant.now()

        val user1 = User(
            id = id,
            googleSub = "google-sub-1",
            email = "user1@example.com",
            name = "User 1",
            createdAt = now,
            updatedAt = now
        )

        val user2 = User(
            id = id,
            googleSub = "google-sub-2",
            email = "user2@example.com",
            name = "User 2",
            createdAt = now,
            updatedAt = now
        )

        assertEquals(user1, user2)
    }

    @Test
    fun `should have different equals for different ids`() {
        val now = Instant.now()

        val user1 = User(
            id = UUID.randomUUID(),
            googleSub = "google-sub-1",
            email = "user@example.com",
            name = "User",
            createdAt = now,
            updatedAt = now
        )

        val user2 = User(
            id = UUID.randomUUID(),
            googleSub = "google-sub-1",
            email = "user@example.com",
            name = "User",
            createdAt = now,
            updatedAt = now
        )

        assertNotEquals(user1, user2)
    }

    @Test
    fun `should have consistent hashCode based on id`() {
        val id = UUID.randomUUID()
        val now = Instant.now()

        val user1 = User(
            id = id,
            googleSub = "google-sub-1",
            email = "user1@example.com",
            name = "User 1",
            createdAt = now,
            updatedAt = now
        )

        val user2 = User(
            id = id,
            googleSub = "google-sub-2",
            email = "user2@example.com",
            name = "User 2",
            createdAt = now,
            updatedAt = now
        )

        assertEquals(user1.hashCode(), user2.hashCode())
    }

    @Test
    fun `should update name`() {
        val now = Instant.now()
        val user = User(
            id = UUID.randomUUID(),
            googleSub = "google-sub-1",
            email = "user@example.com",
            name = "Old Name",
            createdAt = now,
            updatedAt = now
        )

        user.updateName("New Name")

        assertEquals("New Name", user.name)
        assertTrue(user.updatedAt.isAfter(now) || user.updatedAt == now)
    }
}
