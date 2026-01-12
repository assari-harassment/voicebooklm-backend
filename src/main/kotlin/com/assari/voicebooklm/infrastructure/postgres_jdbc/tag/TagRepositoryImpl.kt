package com.assari.voicebooklm.infrastructure.postgres_jdbc.tag

import com.assari.voicebooklm.domain.repository.SortOrder
import com.assari.voicebooklm.domain.repository.TagRepository
import com.assari.voicebooklm.domain.repository.TagSortField
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * タグリポジトリ実装
 *
 * Domain Layer の TagRepository インターフェースを実装する。
 */
@Repository
class TagRepositoryImpl(
    private val tagJdbcRepository: TagJdbcRepository,
) : TagRepository {

    override suspend fun findByUserId(
        userId: UUID,
        sort: TagSortField,
        order: SortOrder,
        limit: Int?,
    ): List<String> {
        return tagJdbcRepository.findByUserId(userId, sort, order, limit)
    }
}
