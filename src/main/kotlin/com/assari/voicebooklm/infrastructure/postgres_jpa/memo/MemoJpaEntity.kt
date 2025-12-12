package com.assari.voicebooklm.infrastructure.postgres_jpa.memo

import com.assari.voicebooklm.domain.model.memo.Memo
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * memos / memo_tags の永続化エンティティ。
 */
@Entity
@Table(name = "memos")
class MemoJpaEntity(
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(name = "user_id", nullable = false)
    var userId: UUID = UUID.randomUUID(),
    @Column(name = "transcription_status", nullable = false)
    var transcriptionStatus: String = "COMPLETED",
    @Column(name = "formatting_status", nullable = false)
    var formattingStatus: String = "COMPLETED",
    @Column(name = "transcription")
    var transcription: String? = null,
    @Column(name = "title")
    var title: String? = null,
    @Column(name = "content")
    var content: String? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
    @Column(name = "deleted", nullable = false)
    var deleted: Boolean = false,
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "memo_tags",
        joinColumns = [JoinColumn(name = "memo_id")],
    )
    @Column(name = "tag", nullable = false)
    var tags: MutableSet<String> = linkedSetOf(),
) {

    @PrePersist
    fun onCreate() {
        val now = Instant.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }

    fun toDomain(): Memo =
        Memo(
            id = id,
            title = title ?: error("title must not be null for Memo ${id}"),
            content = content ?: error("content must not be null for Memo ${id}"),
            tags = tags.toList(),
            userId = userId,
            deleted = deleted,
        )

    companion object {
        fun fromDomain(memo: Memo, transcriptionText: String? = null): MemoJpaEntity =
            MemoJpaEntity(
                id = memo.id,
                userId = memo.userId,
                transcriptionStatus = "COMPLETED",
                formattingStatus = "COMPLETED",
                transcription = transcriptionText,
                title = memo.title,
                content = memo.content,
                deleted = memo.deleted,
                tags = memo.tags.toCollection(linkedSetOf()),
            )
    }
}
