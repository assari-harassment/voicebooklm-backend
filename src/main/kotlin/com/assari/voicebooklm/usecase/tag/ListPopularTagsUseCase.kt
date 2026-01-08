package com.assari.voicebooklm.usecase.tag

import com.assari.voicebooklm.domain.repository.TagRepository
import com.assari.voicebooklm.domain.repository.TagWithCount
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * ユーザーのタグを使用回数順で取得するユースケース
 */
@Service
open class ListPopularTagsUseCase(
    private val tagRepository: TagRepository,
) {
    @Transactional(readOnly = true)
    open suspend fun execute(input: ListPopularTagsInput): ListPopularTagsOutput {
        val tags = tagRepository.findTagsWithCountByUserId(input.userId, input.limit)
        return ListPopularTagsOutput(tags)
    }
}

/**
 * 人気タグ一覧取得 Input
 */
data class ListPopularTagsInput(
    val userId: UUID,
    val limit: Int? = null,
)

/**
 * 人気タグ一覧取得 Output
 */
data class ListPopularTagsOutput(
    val tags: List<TagWithCount>,
)
