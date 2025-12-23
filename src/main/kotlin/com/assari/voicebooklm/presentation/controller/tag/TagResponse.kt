package com.assari.voicebooklm.presentation.controller.tag

import io.swagger.v3.oas.annotations.media.Schema

/**
 * タグ一覧レスポンス
 */
@Schema(description = "タグ一覧レスポンス")
data class TagResponse(
    @Schema(description = "タグ一覧（使用頻度の高い順）")
    val tags: List<TagInfo>
)

/**
 * タグ情報
 */
@Schema(description = "タグ情報")
data class TagInfo(
    @Schema(description = "タグ名", example = "仕事")
    val name: String,

    @Schema(description = "使用回数", example = "5")
    val count: Int
)

