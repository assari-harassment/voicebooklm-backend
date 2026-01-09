package com.assari.voicebooklm.presentation.controller.tag

import com.assari.voicebooklm.usecase.tag.ListTagsOutput
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

/**
 * タグ一覧レスポンス
 */
@Schema(description = "タグ一覧レスポンス")
data class ListTagsResponse(
    @Schema(description = "タグ一覧")
    val tags: List<TagItemResponse>,
) {
    companion object {
        fun from(output: ListTagsOutput): ListTagsResponse {
            val tags = output.tags.map { tag ->
                TagItemResponse(
                    id = tag.id,
                    name = tag.name,
                )
            }
            return ListTagsResponse(tags = tags)
        }
    }
}

/**
 * タグ一覧の要素
 */
@Schema(description = "タグ情報")
data class TagItemResponse(
    @Schema(description = "タグID", example = "01912345-6789-7abc-def0-123456789abc")
    val id: UUID,

    @Schema(description = "タグ名", example = "仕事")
    val name: String,
)
