package com.assari.voicebooklm.infrastructure.postgres_jdbc.tag

import com.assari.voicebooklm.domain.model.Tag
import com.assari.voicebooklm.domain.repository.TagRepository
import com.assari.voicebooklm.domain.repository.TagWithCount
import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * TagRepository の JDBC 実装
 *
 * Domain Layer の TagRepository インターフェースを実装する。
 * - CRUD操作は TagMasterJdbcRepository を使用
 * - カウント集計クエリは JdbcTemplate を使用
 */
@Repository
class TagRepositoryImpl(
    private val tagMasterJdbcRepository: TagMasterJdbcRepository,
    private val jdbcTemplate: JdbcTemplate,
) : TagRepository {

    override suspend fun save(tag: Tag): Tag {
        val entity = TagMasterEntity.fromDomain(tag)
        val savedEntity = tagMasterJdbcRepository.save(entity)
        return savedEntity.toDomain()
    }

    override suspend fun findById(id: UUID): Tag? {
        return tagMasterJdbcRepository.findByIdOrNull(id)?.toDomain()
    }

    override suspend fun findByUserId(userId: UUID): List<Tag> {
        return tagMasterJdbcRepository.findByUserId(userId).map { it.toDomain() }
    }

    override suspend fun findByUserIdAndName(userId: UUID, name: String): Tag? {
        return tagMasterJdbcRepository.findByUserIdAndName(userId, name)?.toDomain()
    }

    override suspend fun findByUserIdAndNames(userId: UUID, names: List<String>): List<Tag> {
        if (names.isEmpty()) return emptyList()
        return tagMasterJdbcRepository.findByUserIdAndNameIn(userId, names).map { it.toDomain() }
    }

    override suspend fun findTagsWithCountByUserId(userId: UUID): List<TagWithCount> {
        // memo_tags の tag_id を集計し、tags マスタと結合してタグ情報を取得
        val sql = """
            SELECT t.id, t.user_id, t.name, t.created_at, t.updated_at, COUNT(mt.id) AS count
            FROM tags t
            LEFT JOIN memo_tags mt ON t.id = mt.tag_id
            LEFT JOIN memos m ON mt.memo_id = m.id AND m.deleted = false
            WHERE t.user_id = ?
            GROUP BY t.id, t.user_id, t.name, t.created_at, t.updated_at
            ORDER BY count DESC, t.name ASC
        """.trimIndent()

        return jdbcTemplate.query(sql, { rs, _ ->
            val tag = Tag.restore(
                id = UUID.fromString(rs.getString("id")),
                userId = UUID.fromString(rs.getString("user_id")),
                name = rs.getString("name"),
                createdAt = rs.getTimestamp("created_at").toInstant(),
                updatedAt = rs.getTimestamp("updated_at").toInstant(),
            )
            TagWithCount(
                tag = tag,
                count = rs.getInt("count"),
            )
        }, userId)
    }

    override suspend fun delete(id: UUID) {
        tagMasterJdbcRepository.deleteById(id)
    }

    override fun deleteByUserId(userId: UUID) {
        tagMasterJdbcRepository.deleteByUserId(userId)
    }
}
