package com.assari.voicebooklm.presentation.controller.profile

import com.assari.voicebooklm.domain.exception.DomainException
import com.assari.voicebooklm.domain.exception.ErrorCode
import com.assari.voicebooklm.usecase.auth.GetCurrentUserInput
import com.assari.voicebooklm.usecase.auth.GetCurrentUserOutput
import com.assari.voicebooklm.usecase.auth.GetCurrentUserUseCase
import io.mockk.every
import io.mockk.mockk
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

/**
 * ProfileController のプロフィール取得を直接呼び出しで検証。
 */
class ProfileControllerTest {

    private lateinit var getCurrentUserUseCase: GetCurrentUserUseCase
    private lateinit var controller: ProfileController

    @BeforeEach
    fun setup() {
        getCurrentUserUseCase = mockk()
        controller = ProfileController(getCurrentUserUseCase)
    }

    @Test
    fun `認証済みユーザーのプロフィール情報を返す`() {
        val userId = UUID.randomUUID()
        val input = GetCurrentUserInput(userId = userId)
        every { getCurrentUserUseCase.execute(input) } returns GetCurrentUserOutput(
            id = userId,
            email = "test@example.com",
            name = "テストユーザー",
        )

        val response = controller.getProfile(userId)

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = requireNotNull(response.body) { "response body should not be null" }
        assertEquals("テストユーザー", body.name)
        assertEquals("test@example.com", body.email)
    }

    @Test
    fun `未認証の場合はResponseStatusExceptionがスローされる`() {
        val exception = assertThrows<ResponseStatusException> {
            controller.getProfile(null)
        }

        assertEquals(HttpStatus.UNAUTHORIZED, exception.statusCode)
    }

    @Test
    fun `ユーザーが見つからない場合はUSER_NOT_FOUND例外がスローされる`() {
        val userId = UUID.randomUUID()
        val input = GetCurrentUserInput(userId = userId)
        every { getCurrentUserUseCase.execute(input) } throws
            DomainException(ErrorCode.USER_NOT_FOUND)

        val exception = assertThrows<DomainException> {
            controller.getProfile(userId)
        }

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.code)
    }
}
