package com.assari.voicebooklm.usecase.tag

import com.assari.voicebooklm.domain.model.SortOrder
import com.assari.voicebooklm.domain.model.TagSortField
import com.assari.voicebooklm.domain.repository.TagRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * ユーザーのタグ一覧を取得するユースケース
 *
 * メモ編集画面などで、既存のタグから選択する際に使用。
 */
@Service
open class ListTagsUseCase(
    private val tagRepository: TagRepository,
) {
    @Transactional(readOnly = true)
    open suspend fun execute(input: ListTagsInput): ListTagsOutput {
        val tags = tagRepository.findByUserId(
            userId = input.userId,
            sort = input.sort,
            order = input.order,
            limit = input.limit,
        )
        return ListTagsOutput(tags = tags)
    }
}

/**
 * タグ一覧取得 Input
 */
data class ListTagsInput(
    val userId: UUID,
    /** ソート基準（デフォルト: NAME） */
    val sort: TagSortField = TagSortField.NAME,
    /** ソート順（デフォルト: ASC） */
    val order: SortOrder = SortOrder.ASC,
    /** 取得件数上限（null の場合は無制限） */
    val limit: Int? = null,
)

/**
 * タグ一覧取得 Output
 */
data class ListTagsOutput(
    val tags: List<String>,
)
