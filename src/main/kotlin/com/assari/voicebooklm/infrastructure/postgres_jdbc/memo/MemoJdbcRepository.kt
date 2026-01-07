package com.assari.voicebooklm.infrastructure.postgres_jdbc.memo

import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Spring Data JDBC メモリポジトリインターフェース
 *
 * 内部使用専用。外部からは VoiceMemoRepository インターフェースを使用する。
 */
@Repository
interface MemoJdbcRepository : CrudRepository<MemoEntity, UUID> {

    /**
     * ID で未削除のメモを取得
     */
    @Query("""
        SELECT * FROM memos
        WHERE id = :id AND deleted = false
    """)
    fun findActiveMemoById(@Param("id") id: UUID): MemoEntity?

    /**
     * ユーザーの未削除メモを作成日時の新しい順で取得
     */
    @Query("""
        SELECT * FROM memos
        WHERE user_id = :userId AND deleted = false
        ORDER BY created_at DESC
    """)
    fun findActiveMemosByUser(@Param("userId") userId: UUID): List<MemoEntity>

    /**
     * ユーザーID に紐づくメモをすべて削除（物理削除）
     */
    @Modifying
    @Query("DELETE FROM memos WHERE user_id = :userId")
    fun deleteByUserId(@Param("userId") userId: UUID)

    /**
     * ユーザーの未削除メモからタグ一覧を取得（使用回数を含む）
     *
     * @param userId ユーザーID
     * @return タグ名と使用回数の結果リスト（使用回数降順→タグ名昇順でソート済み）
     * 
     * 注意: Spring Data JDBCではネイティブクエリの結果をdata classにマッピングする際、
     * カラム名がプロパティ名と一致する必要があります。PostgreSQLでは大文字小文字が区別されるため、
     * エイリアスは小文字で統一しています。
     */
    @Query("""
        SELECT mt.tag AS tagname, COUNT(*) AS tagcount
        FROM memo_tags mt
        INNER JOIN memos m ON mt.memo_id = m.id
        WHERE m.user_id = :userId AND m.deleted = false
        GROUP BY mt.tag
        ORDER BY tagcount DESC, mt.tag ASC
    """)
    fun findTagsWithCountByUserId(@Param("userId") userId: UUID): List<TagCountProjection>
}
