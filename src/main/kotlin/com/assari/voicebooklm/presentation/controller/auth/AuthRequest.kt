package com.assari.voicebooklm.presentation.controller.auth

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

/**
 * Google OAuth ログインリクエスト
 */
@Schema(description = "Google OAuth ログインリクエスト")
data class GoogleAuthRequest(
    @Schema(description = "Google ID トークン", example = "eyJhbGciOiJSUzI1NiIsInR...")
    @field:NotBlank(message = "ID トークンは必須です")
    val idToken: String
)

/**
 * トークンリフレッシュリクエスト
 */
@Schema(description = "トークンリフレッシュリクエスト")
data class RefreshTokenRequest(
    @Schema(description = "リフレッシュトークン", example = "eyJhbGciOiJIUzI1NiIsInR...")
    @field:NotBlank(message = "リフレッシュトークンは必須です")
    val refreshToken: String
)

/**
 * ログアウトリクエスト
 */
@Schema(description = "ログアウトリクエスト")
data class LogoutRequest(
    @Schema(description = "リフレッシュトークン", example = "eyJhbGciOiJIUzI1NiIsInR...")
    @field:NotBlank(message = "リフレッシュトークンは必須です")
    val refreshToken: String
)
