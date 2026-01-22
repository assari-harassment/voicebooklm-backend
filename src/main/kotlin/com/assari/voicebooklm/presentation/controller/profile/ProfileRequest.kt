package com.assari.voicebooklm.presentation.controller.profile

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * プロフィール更新リクエスト
 */
@Schema(description = "プロフィール更新リクエスト")
data class UpdateProfileRequest(
    @Schema(description = "表示名", example = "山田太郎")
    @field:NotBlank(message = "名前は必須です")
    @field:Size(max = 50, message = "名前は50文字以内で入力してください")
    val name: String
)
