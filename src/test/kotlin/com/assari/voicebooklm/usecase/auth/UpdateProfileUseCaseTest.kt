package com.assari.voicebooklm.usecase.auth

import com.assari.voicebooklm.domain.exception.DomainException
import com.assari.voicebooklm.domain.exception.ErrorCode
import com.assari.voicebooklm.domain.model.User
import com.assari.voicebooklm.domain.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class UpdateProfileUseCaseTest {

    private lateinit var updateProfileUseCase: UpdateProfileUseCase
    private lateinit var userRepository: UserRepository

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        updateProfileUseCase = UpdateProfileUseCase(userRepository)
    }

    @Test
    fun `should update profile name successfully`() {
        // Given
        val userId = UUID.randomUUID()
        val oldName = "Old Name"
        val newName = "New Name"
        val user = User(
            id = userId,
            googleSub = "google-sub",
            email = "test@example.com",
            name = oldName,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        every { userRepository.findById(userId) } returns user
        val savedUserSlot = slot<User>()
        every { userRepository.save(capture(savedUserSlot)) } answers { firstArg() }

        // When
        val result = updateProfileUseCase.execute(UpdateProfileInput(userId, newName))

        // Then
        assertEquals(newName, result.name)
        assertEquals(user.email, result.email)

        val savedUser = savedUserSlot.captured
        assertEquals(newName, savedUser.name)
        assertEquals(userId, savedUser.id)

        verify(exactly = 1) { userRepository.findById(userId) }
        verify(exactly = 1) { userRepository.save(any()) }
    }

    @Test
    fun `should throw exception when user not found`() {
        // Given
        val userId = UUID.randomUUID()
        val newName = "New Name"

        every { userRepository.findById(userId) } returns null

        // When & Then
        val exception = assertThrows(DomainException::class.java) {
            updateProfileUseCase.execute(UpdateProfileInput(userId, newName))
        }

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.code)
        verify(exactly = 1) { userRepository.findById(userId) }
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `should preserve other user fields when updating name`() {
        // Given
        val userId = UUID.randomUUID()
        val googleSub = "google-sub-12345"
        val email = "test@example.com"
        val oldName = "Old Name"
        val newName = "New Name"
        val createdAt = Instant.now().minusSeconds(3600)
        val oldUpdatedAt = Instant.now().minusSeconds(1800)

        val user = User(
            id = userId,
            googleSub = googleSub,
            email = email,
            name = oldName,
            createdAt = createdAt,
            updatedAt = oldUpdatedAt
        )

        every { userRepository.findById(userId) } returns user
        val savedUserSlot = slot<User>()
        every { userRepository.save(capture(savedUserSlot)) } answers { firstArg() }

        // When
        updateProfileUseCase.execute(UpdateProfileInput(userId, newName))

        // Then
        val savedUser = savedUserSlot.captured
        assertEquals(userId, savedUser.id)
        assertEquals(googleSub, savedUser.googleSub)
        assertEquals(email, savedUser.email)
        assertEquals(newName, savedUser.name)
        assertEquals(createdAt, savedUser.createdAt)
        // updatedAt should be updated
        assert(savedUser.updatedAt.isAfter(oldUpdatedAt))
    }
}
