package com.assari.voicebooklm.presentation.controller.profile

import com.assari.voicebooklm.presentation.controller.auth.ErrorResponse
import com.assari.voicebooklm.usecase.auth.GetCurrentUserInput
import com.assari.voicebooklm.usecase.auth.GetCurrentUserUseCase
import com.assari.voicebooklm.usecase.auth.UpdateProfileInput
import com.assari.voicebooklm.usecase.auth.UpdateProfileUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

/**
 * プロフィール REST API コントローラー
 *
 * ユーザーのプロフィール情報を取得する。
 */
@RestController
@RequestMapping("/api/profile")
@Tag(name = "Profile", description = "プロフィール関連 API")
class ProfileController(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase
) {
    /**
     * プロフィール情報取得
     */
    @Operation(
        summary = "プロフィール情報取得",
        description = "認証済みユーザーのプロフィール情報（名前・メールアドレス）を取得します。"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "取得成功"),
        ApiResponse(
            responseCode = "401",
            description = "認証失敗",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "ユーザーが見つからない",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @GetMapping
    fun getProfile(@AuthenticationPrincipal userId: UUID?): ResponseEntity<ProfileResponse> {
        if (userId == null) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "認証が必要です")
        }
        val userInfo = getCurrentUserUseCase.execute(GetCurrentUserInput(userId))
        return ResponseEntity.ok(
            ProfileResponse(
                name = userInfo.name,
                email = userInfo.email
            )
        )
    }

    /**
     * プロフィール情報更新
     */
    @Operation(
        summary = "プロフィール情報更新",
        description = "認証済みユーザーのプロフィール情報（名前）を更新します。"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "更新成功"),
        ApiResponse(
            responseCode = "400",
            description = "バリデーションエラー",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "401",
            description = "認証失敗",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "ユーザーが見つからない",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PatchMapping
    fun updateProfile(
        @AuthenticationPrincipal userId: UUID?,
        @Valid @RequestBody request: UpdateProfileRequest
    ): ResponseEntity<ProfileResponse> {
        if (userId == null) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "認証が必要です")
        }
        val result = updateProfileUseCase.execute(
            UpdateProfileInput(
                userId = userId,
                name = request.name.trim()
            )
        )
        return ResponseEntity.ok(
            ProfileResponse(
                name = result.name,
                email = result.email
            )
        )
    }
}
