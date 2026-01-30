package com.assari.voicebooklm.presentation.controller.memo

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

/**
 * メモ更新リクエスト
 *
 * すべてのフィールドはオプション。指定されたフィールドのみ更新される（PATCH）。
 */
data class UpdateMemoRequest(
    @field:Size(max = 100, message = "タイトルは100文字以内で入力してください")
    val title: String? = null,

    val content: String? = null,

    val tags: List<String>? = null,
)

/**
 * 再要約リクエスト
 */
data class ResummarizeRequest(
    @field:NotBlank(message = "編集された文字起こしテキストは必須です")
    val editedTranscription: String,
)

/**
 * 文字起こしテキストをAI整形して保存するリクエスト
 *
 * WebSocketで受信した文字起こしテキストを受け取り、
 * AI整形してメモとして保存する。
 */
data class FormatMemoRequest(
    @field:NotBlank(message = "文字起こしテキストは必須です")
    val transcription: String,

    /** 言語コード（例: ja-JP）。指定がない場合は ja-JP がデフォルト */
    val language: String? = null,

    /**
     * 保存先フォルダーID。
     * 指定時はAIのフォルダ分類は行わずこのフォルダに保存する。
     * folderPath と両方指定された場合は folderId を優先する。
     */
    val folderId: UUID? = null,

    /**
     * 保存先のフォルダーパス（例: "仕事/プロジェクトA"、"趣味"）。
     * 指定時はこのパスに保存し、必要なフォルダは最大3階層まで自動作成する。
     * folderId が指定されている場合は無視される。
     */
    val folderPath: String? = null,

    /**
     * 録音時に付けておきたいタグ。
     * AI提案タグとマージして保存する。null のときはAI提案のみ。
     */
    val tags: List<String>? = null,
)
