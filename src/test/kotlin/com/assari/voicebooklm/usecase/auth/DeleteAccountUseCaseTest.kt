package com.assari.voicebooklm.usecase.auth

import com.assari.voicebooklm.domain.exception.DomainException
import com.assari.voicebooklm.domain.exception.ErrorCode
import com.assari.voicebooklm.domain.model.User
import com.assari.voicebooklm.domain.repository.FolderRepository
import com.assari.voicebooklm.domain.repository.RefreshTokenRepository
import com.assari.voicebooklm.domain.repository.TagRepository
import com.assari.voicebooklm.domain.repository.UserRepository
import com.assari.voicebooklm.domain.repository.VoiceMemoRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class DeleteAccountUseCaseTest {

    private lateinit var deleteAccountUseCase: DeleteAccountUseCase
    private lateinit var userRepository: UserRepository
    private lateinit var voiceMemoRepository: VoiceMemoRepository
    private lateinit var folderRepository: FolderRepository
    private lateinit var tagRepository: TagRepository
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        voiceMemoRepository = mockk()
        folderRepository = mockk()
        tagRepository = mockk()
        refreshTokenRepository = mockk()

        deleteAccountUseCase = DeleteAccountUseCase(
            userRepository = userRepository,
            voiceMemoRepository = voiceMemoRepository,
            folderRepository = folderRepository,
            tagRepository = tagRepository,
            refreshTokenRepository = refreshTokenRepository,
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
        every { voiceMemoRepository.deleteByUserId(userId) } just Runs
        every { folderRepository.deleteByUserId(userId) } just Runs
        every { tagRepository.deleteByUserId(userId) } just Runs
        every { refreshTokenRepository.deleteByUserId(userId) } just Runs
        every { userRepository.deleteById(userId) } just Runs

        // When
        deleteAccountUseCase.execute(DeleteAccountInput(userId))

        // Then
        verifyOrder {
            userRepository.findById(userId)
            voiceMemoRepository.deleteByUserId(userId)
            folderRepository.deleteByUserId(userId)
            tagRepository.deleteByUserId(userId)
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
        val exception = assertThrows(DomainException::class.java) {
            deleteAccountUseCase.execute(DeleteAccountInput(userId))
        }

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.code)

        verify(exactly = 0) { voiceMemoRepository.deleteByUserId(any()) }
        verify(exactly = 0) { folderRepository.deleteByUserId(any()) }
        verify(exactly = 0) { tagRepository.deleteByUserId(any()) }
        verify(exactly = 0) { refreshTokenRepository.deleteByUserId(any()) }
        verify(exactly = 0) { userRepository.deleteById(any()) }
    }

    @Test
    fun `should delete memos before folders before tags before tokens before user`() {
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
        every { voiceMemoRepository.deleteByUserId(userId) } just Runs
        every { folderRepository.deleteByUserId(userId) } just Runs
        every { tagRepository.deleteByUserId(userId) } just Runs
        every { refreshTokenRepository.deleteByUserId(userId) } just Runs
        every { userRepository.deleteById(userId) } just Runs

        // When
        deleteAccountUseCase.execute(DeleteAccountInput(userId))

        // Then - verify order is important for referential integrity
        verifyOrder {
            voiceMemoRepository.deleteByUserId(userId)
            folderRepository.deleteByUserId(userId)
            tagRepository.deleteByUserId(userId)
            refreshTokenRepository.deleteByUserId(userId)
            userRepository.deleteById(userId)
        }
    }
}
