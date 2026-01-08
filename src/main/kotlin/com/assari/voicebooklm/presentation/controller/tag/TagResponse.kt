package com.assari.voicebooklm.presentation.controller.tag

import com.assari.voicebooklm.domain.model.Tag
import com.assari.voicebooklm.usecase.tag.ListPopularTagsOutput
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

/**
 * 人気タグ一覧レスポンス
 */
@Schema(description = "人気タグ一覧レスポンス")
data class ListPopularTagsResponse(
    @Schema(description = "タグ一覧（使用回数の降順でソート）")
    val tags: List<PopularTagItemResponse>,
) {
    companion object {
        fun from(output: ListPopularTagsOutput): ListPopularTagsResponse {
            val tags = output.tags.map { tagWithCount ->
                PopularTagItemResponse(
                    id = tagWithCount.tag.id,
                    name = tagWithCount.tag.name,
                    count = tagWithCount.count,
                )
            }
            return ListPopularTagsResponse(tags = tags)
        }
    }
}

/**
 * 人気タグ一覧の要素
 */
@Schema(description = "人気タグ情報")
data class PopularTagItemResponse(
    @Schema(description = "タグID", example = "01912345-6789-7abc-def0-123456789abc")
    val id: UUID,

    @Schema(description = "タグ名", example = "仕事")
    val name: String,

    @Schema(description = "使用回数（このタグが付けられているメモの数）", example = "5")
    val count: Int,
)
