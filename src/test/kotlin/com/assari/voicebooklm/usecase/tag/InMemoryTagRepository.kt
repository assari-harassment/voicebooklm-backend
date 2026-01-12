package com.assari.voicebooklm.usecase.tag

import com.assari.voicebooklm.domain.model.SortOrder
import com.assari.voicebooklm.domain.model.TagSortField
import com.assari.voicebooklm.domain.repository.TagRepository
import java.util.UUID

/**
 * テスト用のインメモリTagRepository
 *
 * タグデータは (userId, tag) のペアとして保持する。
 * 同じユーザーが同じタグを複数回使用した場合は複数件登録する。
 */
internal class InMemoryTagRepository(
    initialTags: List<Pair<UUID, String>> = emptyList(),
) : TagRepository {
    private val tags = initialTags.toMutableList()

    override suspend fun findByUserId(
        userId: UUID,
        sort: TagSortField,
        order: SortOrder,
        limit: Int?,
    ): List<String> {
        val userTags = tags.filter { it.first == userId }
        val grouped = userTags.groupBy { it.second }
            .map { (tag, occurrences) -> tag to occurrences.size }

        // セカンダリソートキーとしてtag列を追加し、ソート順を安定させる
        val sorted = when (sort) {
            TagSortField.NAME -> {
                when (order) {
                    SortOrder.ASC -> grouped.sortedBy { it.first }
                    SortOrder.DESC -> grouped.sortedByDescending { it.first }
                }
            }
            TagSortField.USAGE_COUNT -> {
                when (order) {
                    SortOrder.ASC -> grouped.sortedWith(compareBy({ it.second }, { it.first }))
                    SortOrder.DESC -> grouped.sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })
                }
            }
        }

        val result = sorted.map { it.first }
        return if (limit != null && limit > 0) result.take(limit) else result
    }

    /**
     * テスト用: タグを追加する
     */
    fun addTag(userId: UUID, tag: String) {
        tags.add(userId to tag)
    }
}
