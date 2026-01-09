package com.assari.voicebooklm.infrastructure.postgres_jdbc.tag

import com.assari.voicebooklm.domain.model.Tag
import com.assari.voicebooklm.domain.repository.SortOrder
import com.assari.voicebooklm.domain.repository.TagRepository
import com.assari.voicebooklm.domain.repository.TagSortField
import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * TagRepository の JDBC 実装
 *
 * Domain Layer の TagRepository インターフェースを実装する。
 */
@Repository
class TagRepositoryImpl(
    private val tagJdbcRepository: TagJdbcRepository,
    private val jdbcTemplate: JdbcTemplate,
) : TagRepository {

    override suspend fun save(tag: Tag): Tag {
        val entity = TagMasterEntity.fromDomain(tag)
        val savedEntity = tagJdbcRepository.save(entity)
        return savedEntity.toDomain()
    }

    override suspend fun findById(id: UUID): Tag? {
        return tagJdbcRepository.findByIdOrNull(id)?.toDomain()
    }

    override suspend fun findByUserId(userId: UUID): List<Tag> {
        return tagJdbcRepository.findByUserIdOrderByNameAsc(userId).map { it.toDomain() }
    }

    override suspend fun findByUserIdWithSort(
        userId: UUID,
        sortField: TagSortField,
        sortOrder: SortOrder,
        limit: Int?,
    ): List<Tag> {
        // ソート条件を構築
        val orderBy = when (sortField) {
            TagSortField.NAME -> "name"
            TagSortField.USAGE -> "usage_count"
        }
        val orderDirection = when (sortOrder) {
            SortOrder.ASC -> "ASC"
            SortOrder.DESC -> "DESC"
        }
        // usage_countの場合は第2ソートキーとしてnameを追加
        val secondarySort = if (sortField == TagSortField.USAGE) ", name ASC" else ""

        // 動的SQLを構築（LIMIT句をDB側で適用）
        val sql = buildString {
            append("SELECT * FROM tags WHERE user_id = ? ORDER BY $orderBy $orderDirection$secondarySort")
            if (limit != null) {
                append(" LIMIT $limit")
            }
        }

        val entities = jdbcTemplate.query(sql, TagMasterEntity.rowMapper(), userId)
        return entities.map { it.toDomain() }
    }

    override suspend fun findByUserIdAndName(userId: UUID, name: String): Tag? {
        return tagJdbcRepository.findByUserIdAndName(userId, name)?.toDomain()
    }

    override suspend fun findByUserIdAndNames(userId: UUID, names: List<String>): List<Tag> {
        if (names.isEmpty()) return emptyList()
        return tagJdbcRepository.findByUserIdAndNameIn(userId, names).map { it.toDomain() }
    }

    override suspend fun findByIds(ids: List<UUID>): List<Tag> {
        if (ids.isEmpty()) return emptyList()
        return tagJdbcRepository.findByIdIn(ids).map { it.toDomain() }
    }

    override suspend fun delete(id: UUID) {
        tagJdbcRepository.deleteById(id)
    }

    override fun deleteByUserId(userId: UUID) {
        tagJdbcRepository.deleteByUserId(userId)
    }
}
