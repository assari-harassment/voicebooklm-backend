package com.assari.voicebooklm.usecase.memo

import com.assari.voicebooklm.domain.model.TagUsage
import com.assari.voicebooklm.domain.repository.VoiceMemoRepository
import java.util.UUID

/**
 * タグ一覧取得ユースケース
 *
 * 認証済みユーザーが所有するメモのタグ一覧を取得し、
 * 使用頻度の高い順にソートして返す。
 */
open class GetTagsUseCase(
    private val voiceMemoRepository: VoiceMemoRepository,
) {
    /**
     * タグ一覧を取得する
     *
     * @param input タグ取得Input（ユーザーID）
     * @return タグ一覧（使用頻度の高い順）
     */
    open suspend fun execute(input: GetTagsInput): GetTagsOutput {
        val tagUsages = voiceMemoRepository.findTagsWithCountByUserId(input.userId)
        val tagInfoList = tagUsages.map { tagUsage ->
            TagWithCount(name = tagUsage.name, count = tagUsage.count)
        }
        return GetTagsOutput(tags = tagInfoList)
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Input / Output
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * タグ取得Input
 */
data class GetTagsInput(
    val userId: UUID
)

/**
 * タグ取得Output
 */
data class GetTagsOutput(
    val tags: List<TagWithCount>
)

/**
 * タグ情報（名前と使用回数）
 */
data class TagWithCount(
    val name: String,
    val count: Int
)

