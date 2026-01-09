package com.assari.voicebooklm.infrastructure.postgres_jdbc.memo

import com.assari.voicebooklm.domain.model.Formatting
import com.assari.voicebooklm.domain.model.FormattingStatus
import com.assari.voicebooklm.domain.model.Transcription
import com.assari.voicebooklm.domain.model.TranscriptionStatus
import com.assari.voicebooklm.domain.model.VoiceMemo
import com.assari.voicebooklm.infrastructure.postgres_jdbc.tag.TagEntity
import org.slf4j.LoggerFactory
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

/**
 * メモ JDBC エンティティ
 *
 * memos テーブルにマッピングされる。
 * memo_tags との関連は @MappedCollection で管理される（Aggregate Root パターン）。
 */
@Table("memos")
data class MemoEntity(
    @Id
    val id: UUID,

    @Column("user_id")
    val userId: UUID,

    @Column("transcription_status")
    val transcriptionStatus: String,

    @Column("formatting_status")
    val formattingStatus: String,

    val transcription: String?,

    @Column("language_code")
    val languageCode: String,

    @Column("transcription_fallback_used")
    val transcriptionFallbackUsed: Boolean,

    @Column("formatting_fallback_used")
    val formattingFallbackUsed: Boolean,

    val title: String?,

    val content: String?,

    @Column("created_at")
    val createdAt: Instant,

    @Column("updated_at")
    val updatedAt: Instant,

    val deleted: Boolean,

    @Version
    val version: Long? = null,

    @Column("folder_id")
    val folderId: UUID? = null,

    @MappedCollection(idColumn = "memo_id")
    val tags: Set<TagEntity> = emptySet()
) {
    /**
     * VoiceMemo ドメインモデルに変換
     */
    fun toDomain(): VoiceMemo {
        val transcriptionDomain = when (TranscriptionStatus.valueOf(transcriptionStatus)) {
            TranscriptionStatus.PENDING -> Transcription.pending(languageCode)
            TranscriptionStatus.PROCESSING -> Transcription.processing(languageCode)
            TranscriptionStatus.COMPLETED -> {
                if (transcription.isNullOrBlank()) {
                    logger.warn(
                        "Memo id={} has COMPLETED transcription status but transcription text is null/blank. Treating as FAILED.",
                        id
                    )
                    Transcription.failed(languageCode)
                } else {
                    Transcription.completed(
                        text = transcription,
                        languageCode = languageCode,
                        fallbackUsed = transcriptionFallbackUsed,
                    )
                }
            }
            TranscriptionStatus.FAILED -> Transcription.failed(languageCode)
        }

        val formattingDomain = when (FormattingStatus.valueOf(formattingStatus)) {
            FormattingStatus.PENDING -> Formatting.pending()
            FormattingStatus.PROCESSING -> Formatting.processing()
            FormattingStatus.COMPLETED -> {
                if (content.isNullOrBlank()) {
                    logger.warn(
                        "Memo id={} has COMPLETED formatting status but content is null/blank. Treating as FAILED.",
                        id
                    )
                    Formatting.failed()
                } else {
                    Formatting.completed(
                        title = title ?: "Untitled",
                        content = content,
                        tagIds = tags.map { it.tagId },
                        fallbackUsed = formattingFallbackUsed,
                        folderId = folderId,
                    )
                }
            }
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
        private val logger = LoggerFactory.getLogger(MemoEntity::class.java)

        /**
         * VoiceMemo ドメインモデルからエンティティを作成
         */
        fun fromDomain(voiceMemo: VoiceMemo, version: Long? = null): MemoEntity =
            MemoEntity(
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
                // version が null → INSERT、非null → UPDATE（Spring Data JDBC のルール）
                version = version,
                folderId = voiceMemo.formatting.folderId,
                tags = voiceMemo.formatting.tagIds.map { TagEntity.create(it) }.toSet(),
                createdAt = voiceMemo.createdAt,
                updatedAt = voiceMemo.updatedAt,
            )
    }
}
