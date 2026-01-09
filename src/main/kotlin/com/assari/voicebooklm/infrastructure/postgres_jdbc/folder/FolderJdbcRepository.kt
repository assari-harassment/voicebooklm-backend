package com.assari.voicebooklm.infrastructure.postgres_jdbc.folder

import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Spring Data JDBC フォルダーリポジトリインターフェース
 *
 * 内部使用専用。外部からは FolderRepository インターフェースを使用する。
 */
@Repository
interface FolderJdbcRepository : CrudRepository<FolderEntity, UUID> {

    @Query("SELECT * FROM folders WHERE user_id = :userId ORDER BY name")
    fun findByUserId(@Param("userId") userId: UUID): List<FolderEntity>

    @Query("SELECT * FROM folders WHERE user_id = :userId AND parent_id = :parentId ORDER BY name")
    fun findByUserIdAndParentId(
        @Param("userId") userId: UUID,
        @Param("parentId") parentId: UUID
    ): List<FolderEntity>

    @Query("SELECT * FROM folders WHERE user_id = :userId AND parent_id IS NULL ORDER BY name")
    fun findByUserIdAndParentIdIsNull(@Param("userId") userId: UUID): List<FolderEntity>

    @Query("SELECT * FROM folders WHERE user_id = :userId AND parent_id = :parentId AND name = :name")
    fun findByUserIdAndParentIdAndName(
        @Param("userId") userId: UUID,
        @Param("parentId") parentId: UUID,
        @Param("name") name: String
    ): FolderEntity?

    @Query("SELECT * FROM folders WHERE user_id = :userId AND parent_id IS NULL AND name = :name")
    fun findByUserIdAndParentIdIsNullAndName(
        @Param("userId") userId: UUID,
        @Param("name") name: String
    ): FolderEntity?

    @Query("SELECT COUNT(*) > 0 FROM folders WHERE user_id = :userId AND parent_id = :parentId AND name = :name")
    fun existsByUserIdAndParentIdAndName(
        @Param("userId") userId: UUID,
        @Param("parentId") parentId: UUID,
        @Param("name") name: String
    ): Boolean

    @Query("SELECT COUNT(*) > 0 FROM folders WHERE user_id = :userId AND parent_id IS NULL AND name = :name")
    fun existsByUserIdAndParentIdIsNullAndName(
        @Param("userId") userId: UUID,
        @Param("name") name: String
    ): Boolean

    @Query("SELECT COUNT(*) > 0 FROM folders WHERE user_id = :userId AND parent_id = :parentId AND name = :name AND id != :excludeId")
    fun existsByUserIdAndParentIdAndNameExcludingId(
        @Param("userId") userId: UUID,
        @Param("parentId") parentId: UUID,
        @Param("name") name: String,
        @Param("excludeId") excludeId: UUID
    ): Boolean

    @Query("SELECT COUNT(*) > 0 FROM folders WHERE user_id = :userId AND parent_id IS NULL AND name = :name AND id != :excludeId")
    fun existsByUserIdAndParentIdIsNullAndNameExcludingId(
        @Param("userId") userId: UUID,
        @Param("name") name: String,
        @Param("excludeId") excludeId: UUID
    ): Boolean

    @Modifying
    @Query("DELETE FROM folders WHERE user_id = :userId")
    fun deleteByUserId(@Param("userId") userId: UUID)
}
