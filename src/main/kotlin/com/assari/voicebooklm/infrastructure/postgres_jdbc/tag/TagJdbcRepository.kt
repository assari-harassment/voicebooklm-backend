package com.assari.voicebooklm.infrastructure.postgres_jdbc.tag

import com.assari.voicebooklm.domain.repository.SortOrder
import com.assari.voicebooklm.domain.repository.TagSortField
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * タグ検索・集計用JDBCリポジトリ
 *
 * memo_tags テーブルから集計クエリを実行する。
 * 動的SQLが必要なため、NamedParameterJdbcTemplateを使用。
 */
@Repository
class TagJdbcRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) {
    /**
     * ユーザーが使用している全タグを取得する
     *
     * @param userId ユーザーID
     * @param sort ソート基準
     * @param order ソート順
     * @param limit 取得件数上限（null の場合は無制限）
     * @return タグ名のリスト
     */
    fun findByUserId(
        userId: UUID,
        sort: TagSortField,
        order: SortOrder,
        limit: Int?,
    ): List<String> {
        val orderByClause = when (sort) {
            TagSortField.NAME -> "tag"
            TagSortField.USAGE_COUNT -> "usage_count"
        }
        val orderDirection = when (order) {
            SortOrder.ASC -> "ASC"
            SortOrder.DESC -> "DESC"
        }

        val limitClause = if (limit != null && limit > 0) "LIMIT :limit" else ""

        val sql = """
            SELECT mt.tag, COUNT(*) as usage_count
            FROM memo_tags mt
            JOIN memos m ON mt.memo_id = m.id
            WHERE m.user_id = :userId AND m.deleted = FALSE
            GROUP BY mt.tag
            ORDER BY $orderByClause $orderDirection
            $limitClause
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("userId", userId)
        if (limit != null && limit > 0) {
            params.addValue("limit", limit)
        }

        return jdbcTemplate.query(sql, params) { rs, _ ->
            rs.getString("tag")
        }
    }
}
