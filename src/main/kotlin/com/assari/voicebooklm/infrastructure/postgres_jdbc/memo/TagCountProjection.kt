package com.assari.voicebooklm.infrastructure.postgres_jdbc.memo

/**
 * タグ一覧取得用のネイティブクエリ結果DTO
 *
 * Spring Data JDBCではInterface Projectionがネイティブクエリでサポートされていないため、
 * data classを使用してネイティブクエリの結果を名前ベースでマッピングする。
 * SQLクエリの`AS tagName`と`AS tagCount`エイリアスに対応する。
 */
data class TagCountProjection(
    /**
     * タグ名（SQLクエリの`AS tagname`に対応）
     * PostgreSQLでは大文字小文字が区別されるため、小文字のエイリアスを使用
     */
    val tagname: String,

    /**
     * タグの使用回数（SQLクエリの`AS tagcount`に対応）
     * PostgreSQLでは大文字小文字が区別されるため、小文字のエイリアスを使用
     */
    val tagcount: Long,
)

