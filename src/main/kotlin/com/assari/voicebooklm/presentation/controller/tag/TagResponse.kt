package com.assari.voicebooklm.presentation.controller.tag

import io.swagger.v3.oas.annotations.media.Schema

/**
 * タグ一覧レスポンス
 */
@Schema(description = "タグ一覧レスポンス")
data class TagsResponse(
    @Schema(description = "タグ名のリスト", example = "[\"開発\", \"コード\", \"ミーティング\"]")
    val tags: List<String>,
)
