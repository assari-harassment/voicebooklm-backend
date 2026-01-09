package com.assari.voicebooklm.usecase.tag

import com.assari.voicebooklm.domain.model.Tag
import com.assari.voicebooklm.domain.repository.SortOrder
import com.assari.voicebooklm.domain.repository.TagRepository
import com.assari.voicebooklm.domain.repository.TagSortField
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * ユーザーのタグ一覧を取得するユースケース
 */
@Service
open class ListTagsUseCase(
    private val tagRepository: TagRepository,
) {
    @Transactional(readOnly = true)
    open suspend fun execute(input: ListTagsInput): ListTagsOutput {
        val tags = tagRepository.findByUserIdWithSort(
            userId = input.userId,
            sortField = input.sort,
            sortOrder = input.order,
            limit = input.limit,
        )
        return ListTagsOutput(tags)
    }
}

/**
 * タグ一覧取得 Input
 */
data class ListTagsInput(
    val userId: UUID,
    val sort: TagSortField = TagSortField.NAME,
    val order: SortOrder = SortOrder.ASC,
    val limit: Int? = null,
)

/**
 * タグ一覧取得 Output
 */
data class ListTagsOutput(
    val tags: List<Tag>,
)
