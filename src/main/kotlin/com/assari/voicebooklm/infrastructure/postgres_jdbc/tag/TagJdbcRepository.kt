package com.assari.voicebooklm.infrastructure.postgres_jdbc.tag

import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Spring Data JDBC タグリポジトリインターフェース
 *
 * 内部使用専用。外部からは TagRepository インターフェースを使用する。
 */
@Repository
interface TagJdbcRepository : CrudRepository<TagMasterEntity, UUID> {

    @Query("SELECT * FROM tags WHERE user_id = :userId ORDER BY name ASC")
    fun findByUserIdOrderByNameAsc(@Param("userId") userId: UUID): List<TagMasterEntity>

    @Query("SELECT * FROM tags WHERE user_id = :userId ORDER BY name DESC")
    fun findByUserIdOrderByNameDesc(@Param("userId") userId: UUID): List<TagMasterEntity>

    @Query("SELECT * FROM tags WHERE user_id = :userId ORDER BY usage_count ASC, name ASC")
    fun findByUserIdOrderByUsageCountAsc(@Param("userId") userId: UUID): List<TagMasterEntity>

    @Query("SELECT * FROM tags WHERE user_id = :userId ORDER BY usage_count DESC, name ASC")
    fun findByUserIdOrderByUsageCountDesc(@Param("userId") userId: UUID): List<TagMasterEntity>

    @Query("SELECT * FROM tags WHERE user_id = :userId AND name = :name")
    fun findByUserIdAndName(
        @Param("userId") userId: UUID,
        @Param("name") name: String
    ): TagMasterEntity?

    @Query("SELECT * FROM tags WHERE user_id = :userId AND name IN (:names)")
    fun findByUserIdAndNameIn(
        @Param("userId") userId: UUID,
        @Param("names") names: List<String>
    ): List<TagMasterEntity>

    @Query("SELECT * FROM tags WHERE id IN (:ids)")
    fun findByIdIn(@Param("ids") ids: List<UUID>): List<TagMasterEntity>

    @Modifying
    @Query("DELETE FROM tags WHERE user_id = :userId")
    fun deleteByUserId(@Param("userId") userId: UUID)
}
