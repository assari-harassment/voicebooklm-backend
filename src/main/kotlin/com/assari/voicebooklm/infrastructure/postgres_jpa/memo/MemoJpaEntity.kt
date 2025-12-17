package com.assari.voicebooklm.infrastructure.postgres_jpa.memo

import com.assari.voicebooklm.domain.model.Formatting
import com.assari.voicebooklm.domain.model.FormattingStatus
import com.assari.voicebooklm.domain.model.Transcription
import com.assari.voicebooklm.domain.model.TranscriptionStatus
import com.assari.voicebooklm.domain.model.VoiceMemo
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
 * memos / memo_tags の永続化エンティティ
 */
@Entity
@Table(name = "memos")
class MemoJpaEntity(
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(name = "user_id", nullable = false)
    var userId: UUID = UUID.randomUUID(),
    @Column(name = "transcription_status", nullable = false)
    var transcriptionStatus: String = "PENDING",
    @Column(name = "formatting_status", nullable = false)
    var formattingStatus: String = "PENDING",
    @Column(name = "transcription")
    var transcription: String? = null,
    @Column(name = "language_code")
    var languageCode: String = "ja-JP",
    @Column(name = "transcription_fallback_used")
    var transcriptionFallbackUsed: Boolean = false,
    @Column(name = "formatting_fallback_used")
    var formattingFallbackUsed: Boolean = false,
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

    /**
     * VoiceMemo ドメインモデルに変換
     */
    fun toDomain(): VoiceMemo {
        val transcriptionDomain = when (TranscriptionStatus.valueOf(transcriptionStatus)) {
            TranscriptionStatus.PENDING -> Transcription.pending(languageCode)
            TranscriptionStatus.PROCESSING -> Transcription.processing(languageCode)
            TranscriptionStatus.COMPLETED -> Transcription.completed(
                text = transcription ?: "",
                languageCode = languageCode,
                fallbackUsed = transcriptionFallbackUsed,
            )
            TranscriptionStatus.FAILED -> Transcription.failed(languageCode)
        }

        val formattingDomain = when (FormattingStatus.valueOf(formattingStatus)) {
            FormattingStatus.PENDING -> Formatting.pending()
            FormattingStatus.PROCESSING -> Formatting.processing()
            FormattingStatus.COMPLETED -> Formatting.completed(
                title = title ?: "Untitled",
                content = content ?: "",
                tags = tags.toList(),
                fallbackUsed = formattingFallbackUsed,
            )
            FormattingStatus.FAILED -> Formatting.failed()
        }

        return VoiceMemo.restore(
            id = id,
            userId = userId,
            transcription = transcriptionDomain,
            formatting = formattingDomain,
            deleted = deleted,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    companion object {
        /**
         * VoiceMemo ドメインモデルからエンティティを作成
         */
        fun fromDomain(voiceMemo: VoiceMemo): MemoJpaEntity =
            MemoJpaEntity(
                id = voiceMemo.id,
                userId = voiceMemo.userId,
                transcriptionStatus = voiceMemo.transcription.status.name,
                formattingStatus = voiceMemo.formatting.status.name,
                transcription = voiceMemo.transcription.text,
                languageCode = voiceMemo.transcription.languageCode,
                transcriptionFallbackUsed = voiceMemo.transcription.fallbackUsed,
                formattingFallbackUsed = voiceMemo.formatting.fallbackUsed,
                title = voiceMemo.formatting.title,
                content = voiceMemo.formatting.content,
                deleted = voiceMemo.deleted,
                tags = voiceMemo.formatting.tags.toCollection(linkedSetOf()),
                createdAt = voiceMemo.createdAt,
                updatedAt = voiceMemo.updatedAt,
            )
    }
}
