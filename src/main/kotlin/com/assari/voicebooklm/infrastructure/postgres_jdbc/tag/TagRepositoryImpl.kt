package com.assari.voicebooklm.infrastructure.postgres_jdbc.tag

import com.assari.voicebooklm.domain.model.Tag
import com.assari.voicebooklm.domain.repository.SortOrder
import com.assari.voicebooklm.domain.repository.TagRepository
import com.assari.voicebooklm.domain.repository.TagSortField
import org.springframework.data.repository.findByIdOrNull
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
        val entities = when (sortField) {
            TagSortField.NAME -> when (sortOrder) {
                SortOrder.ASC -> tagJdbcRepository.findByUserIdOrderByNameAsc(userId)
                SortOrder.DESC -> tagJdbcRepository.findByUserIdOrderByNameDesc(userId)
            }
            TagSortField.USAGE -> when (sortOrder) {
                SortOrder.ASC -> tagJdbcRepository.findByUserIdOrderByUsageCountAsc(userId)
                SortOrder.DESC -> tagJdbcRepository.findByUserIdOrderByUsageCountDesc(userId)
            }
        }
        val tags = entities.map { it.toDomain() }
        return if (limit != null) tags.take(limit) else tags
    }

    override suspend fun findByUserIdAndName(userId: UUID, name: String): Tag? {
        return tagJdbcRepository.findByUserIdAndName(userId, name)?.toDomain()
    }

    override suspend fun findByUserIdAndNames(userId: UUID, names: List<String>): List<Tag> {
        if (names.isEmpty()) return emptyList()
        return tagJdbcRepository.findByUserIdAndNameIn(userId, names).map { it.toDomain() }
    }

    override suspend fun delete(id: UUID) {
        tagJdbcRepository.deleteById(id)
    }

    override fun deleteByUserId(userId: UUID) {
        tagJdbcRepository.deleteByUserId(userId)
    }
}
