package com.assari.voicebooklm.usecase.tag

import com.assari.voicebooklm.domain.repository.TagRepository
import com.assari.voicebooklm.domain.repository.TagWithCount
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * ユーザーのタグ一覧を使用回数順で取得するユースケース
 */
@Service
open class ListTagsUseCase(
    private val tagRepository: TagRepository,
) {
    @Transactional(readOnly = true)
    open suspend fun execute(input: ListTagsInput): ListTagsOutput {
        val tags = tagRepository.findTagsWithCountByUserId(input.userId)
        return ListTagsOutput(tags)
    }
}

/**
 * タグ一覧取得 Input
 */
data class ListTagsInput(
    val userId: UUID,
)

/**
 * タグ一覧取得 Output
 */
data class ListTagsOutput(
    val tags: List<TagWithCount>,
)
