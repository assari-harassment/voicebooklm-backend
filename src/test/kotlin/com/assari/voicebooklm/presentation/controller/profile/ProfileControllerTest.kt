package com.assari.voicebooklm.presentation.controller.profile

import com.assari.voicebooklm.domain.exception.DomainException
import com.assari.voicebooklm.domain.exception.ErrorCode
import com.assari.voicebooklm.usecase.auth.GetCurrentUserInput
import com.assari.voicebooklm.usecase.auth.GetCurrentUserOutput
import com.assari.voicebooklm.usecase.auth.GetCurrentUserUseCase
import com.assari.voicebooklm.usecase.auth.UpdateProfileInput
import com.assari.voicebooklm.usecase.auth.UpdateProfileOutput
import com.assari.voicebooklm.usecase.auth.UpdateProfileUseCase
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
 * ProfileController の取得・更新を直接呼び出しで検証。
 */
class ProfileControllerTest {

    private lateinit var getCurrentUserUseCase: GetCurrentUserUseCase
    private lateinit var updateProfileUseCase: UpdateProfileUseCase
    private lateinit var controller: ProfileController

    @BeforeEach
    fun setup() {
        getCurrentUserUseCase = mockk()
        updateProfileUseCase = mockk()
        controller = ProfileController(
            getCurrentUserUseCase = getCurrentUserUseCase,
            updateProfileUseCase = updateProfileUseCase,
        )
    }

    // ===== getProfile エンドポイントのテスト =====

    @Test
    fun `認証済みユーザーのプロフィール情報を返す`() {
        val userId = UUID.randomUUID()
        val input = GetCurrentUserInput(userId = userId)
        every { getCurrentUserUseCase.execute(input) } returns GetCurrentUserOutput(
            id = userId,
            email = "test@example.com",
            name = "田中太郎",
        )

        val response = controller.getProfile(userId)

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = requireNotNull(response.body) { "response body should not be null" }
        assertEquals("田中太郎", body.name)
        assertEquals("test@example.com", body.email)
    }

    @Test
    fun `getProfile 未認証の場合はResponseStatusExceptionがスローされる`() {
        val exception = assertThrows<ResponseStatusException> {
            controller.getProfile(null)
        }

        assertEquals(HttpStatus.UNAUTHORIZED, exception.statusCode)
    }

    @Test
    fun `getProfile ユーザーが見つからない場合はUSER_NOT_FOUND例外がスローされる`() {
        val userId = UUID.randomUUID()
        val input = GetCurrentUserInput(userId = userId)
        every { getCurrentUserUseCase.execute(input) } throws
            DomainException(ErrorCode.USER_NOT_FOUND)

        val exception = assertThrows<DomainException> {
            controller.getProfile(userId)
        }

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.code)
    }

    // ===== updateProfile エンドポイントのテスト =====

    @Test
    fun `認証済みユーザーがプロフィールを更新すると200が返る`() {
        val userId = UUID.randomUUID()
        val request = UpdateProfileRequest(name = "山田花子")
        val input = UpdateProfileInput(
            userId = userId,
            name = "山田花子",
        )
        every { updateProfileUseCase.execute(input) } returns UpdateProfileOutput(
            name = "山田花子",
            email = "test@example.com",
        )

        val response = controller.updateProfile(userId, request)

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = requireNotNull(response.body) { "response body should not be null" }
        assertEquals("山田花子", body.name)
        assertEquals("test@example.com", body.email)
    }

    @Test
    fun `認証済みユーザーが前後に空白のある名前でプロフィールを更新するとトリムされて200が返る`() {
        val userId = UUID.randomUUID()
        val request = UpdateProfileRequest(name = "  山田花子  ")
        val input = UpdateProfileInput(
            userId = userId,
            name = "山田花子",
        )
        every { updateProfileUseCase.execute(input) } returns UpdateProfileOutput(
            name = "山田花子",
            email = "test@example.com",
        )

        val response = controller.updateProfile(userId, request)

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = requireNotNull(response.body) { "response body should not be null" }
        assertEquals("山田花子", body.name)
        assertEquals("test@example.com", body.email)
    }

    @Test
    fun `updateProfile 未認証の場合はResponseStatusExceptionがスローされる`() {
        val request = UpdateProfileRequest(name = "山田花子")

        val exception = assertThrows<ResponseStatusException> {
            controller.updateProfile(null, request)
        }

        assertEquals(HttpStatus.UNAUTHORIZED, exception.statusCode)
    }

    @Test
    fun `updateProfile ユーザーが見つからない場合はUSER_NOT_FOUND例外がスローされる`() {
        val userId = UUID.randomUUID()
        val request = UpdateProfileRequest(name = "山田花子")
        val input = UpdateProfileInput(
            userId = userId,
            name = "山田花子",
        )
        every { updateProfileUseCase.execute(input) } throws
            DomainException(ErrorCode.USER_NOT_FOUND)

        val exception = assertThrows<DomainException> {
            controller.updateProfile(userId, request)
        }

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.code)
    }
}
