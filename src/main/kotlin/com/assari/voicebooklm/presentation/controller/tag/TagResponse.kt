package com.assari.voicebooklm.presentation.controller.tag

import com.assari.voicebooklm.usecase.memo.GetTagsOutput
import io.swagger.v3.oas.annotations.media.Schema

/**
 * タグ一覧レスポンス
 */
@Schema(description = "タグ一覧レスポンス")
data class TagResponse(
    @Schema(description = "タグ一覧（使用頻度の高い順）")
    val tags: List<TagInfo>
) {
    companion object {
        /**
         * UseCaseの出力からレスポンスDTOに変換
         */
        fun from(result: GetTagsOutput): TagResponse {
            return TagResponse(
                tags = result.tags.map { it.toTagInfo() }
            )
        }
    }
}

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

/**
 * UseCaseのTagWithCountからレスポンスDTOのTagInfoへの変換拡張関数
 *
 * TagWithCountとTagInfoの構造が変わった場合は、このメソッドのみ修正すればよいように集約しています。
 */
private fun com.assari.voicebooklm.usecase.memo.TagWithCount.toTagInfo(): TagInfo =
    TagInfo(name = name, count = count)

