package com.assari.voicebooklm.usecase.auth

import com.assari.voicebooklm.domain.model.User
import com.assari.voicebooklm.domain.repository.MemoRepository
import com.assari.voicebooklm.domain.repository.RefreshTokenRepository
import com.assari.voicebooklm.domain.repository.UserRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class DeleteAccountUseCaseTest {

    private lateinit var deleteAccountUseCase: DeleteAccountUseCase
    private lateinit var userRepository: UserRepository
    private lateinit var memoRepository: MemoRepository
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        memoRepository = mockk()
        refreshTokenRepository = mockk()

        deleteAccountUseCase = DeleteAccountUseCase(
            userRepository = userRepository,
            memoRepository = memoRepository,
            refreshTokenRepository = refreshTokenRepository
        )
    }

    @Test
    fun `should delete account successfully`() {
        // Given
        val userId = UUID.randomUUID()
        val user = User(
            id = userId,
            googleSub = "google-sub",
            email = "test@example.com",
            name = "Test User",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        every { userRepository.findById(userId) } returns user
        every { memoRepository.deleteByUserId(userId) } just Runs
        every { refreshTokenRepository.deleteByUserId(userId) } just Runs
        every { userRepository.deleteById(userId) } just Runs

        // When
        deleteAccountUseCase.execute(DeleteAccountCommand(userId))

        // Then
        verifyOrder {
            userRepository.findById(userId)
            memoRepository.deleteByUserId(userId)
            refreshTokenRepository.deleteByUserId(userId)
            userRepository.deleteById(userId)
        }
    }

    @Test
    fun `should throw exception when user not found`() {
        // Given
        val userId = UUID.randomUUID()

        every { userRepository.findById(userId) } returns null

        // When & Then
        val exception = assertThrows(UserNotFoundException::class.java) {
            deleteAccountUseCase.execute(DeleteAccountCommand(userId))
        }

        assertEquals("ユーザーが見つかりません", exception.message)

        verify(exactly = 0) { memoRepository.deleteByUserId(any()) }
        verify(exactly = 0) { refreshTokenRepository.deleteByUserId(any()) }
        verify(exactly = 0) { userRepository.deleteById(any()) }
    }

    @Test
    fun `should delete memos before tokens before user`() {
        // Given
        val userId = UUID.randomUUID()
        val user = User(
            id = userId,
            googleSub = "google-sub",
            email = "test@example.com",
            name = "Test User",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        every { userRepository.findById(userId) } returns user
        every { memoRepository.deleteByUserId(userId) } just Runs
        every { refreshTokenRepository.deleteByUserId(userId) } just Runs
        every { userRepository.deleteById(userId) } just Runs

        // When
        deleteAccountUseCase.execute(DeleteAccountCommand(userId))

        // Then - verify order is important for referential integrity
        verifyOrder {
            memoRepository.deleteByUserId(userId)
            refreshTokenRepository.deleteByUserId(userId)
            userRepository.deleteById(userId)
        }
    }
}
