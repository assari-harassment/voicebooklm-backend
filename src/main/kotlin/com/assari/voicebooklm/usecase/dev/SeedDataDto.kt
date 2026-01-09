package com.assari.voicebooklm.usecase.dev

/**
 * seed-data.yml のルート構造
 */
data class SeedData(
    val folders: List<SeedFolder> = emptyList(),
    val memos: List<SeedMemo> = emptyList(),
)

/**
 * フォルダー定義
 */
data class SeedFolder(
    val name: String,
    val children: List<SeedFolder> = emptyList(),
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

/**
 * メモ定義
 */
data class SeedMemo(
    /** フォルダーパス（例: "仕事/会議"）。null の場合は未分類 */
    val folder: String? = null,
    val title: String,
    val transcription: String,
    val content: String,
    val tags: List<String> = emptyList(),
    val createdAt: String? = null,
    val updatedAt: String? = null,
)
