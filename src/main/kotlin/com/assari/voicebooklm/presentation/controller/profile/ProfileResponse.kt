package com.assari.voicebooklm.presentation.controller.profile

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

/**
 * プロフィール情報レスポンス
 */
@Schema(description = "プロフィール情報レスポンス")
data class ProfileResponse(
    @Schema(description = "ユーザー名", example = "田中太郎")
    val name: String,

    @Schema(description = "メールアドレス", example = "user@example.com")
    val email: String
)
