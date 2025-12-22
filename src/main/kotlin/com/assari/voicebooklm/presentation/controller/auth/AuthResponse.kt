package com.assari.voicebooklm.presentation.controller.auth

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

/**
 * ユーザー情報レスポンス
 */
@Schema(description = "ユーザー情報レスポンス")
data class UserResponse(
    @Schema(description = "ユーザー ID", example = "01916f54-1234-7abc-def0-123456789abc")
    val id: UUID,

    @Schema(description = "メールアドレス", example = "user@example.com")
    val email: String,

    @Schema(description = "ユーザー名", example = "田中太郎")
    val name: String
)

/**
 * エラーレスポンス
 */
@Schema(description = "エラーレスポンス")
data class ErrorResponse(
    @Schema(description = "エラーメッセージ", example = "認証に失敗しました")
    val error: String,

    @Schema(description = "エラーコード", example = "UNAUTHORIZED")
    val code: String
)
