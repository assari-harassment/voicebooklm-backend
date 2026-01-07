package com.assari.voicebooklm.domain.model

/**
 * タグの使用状況を表すドメインモデル
 *
 * タグ名とその使用回数を保持し、人気のタグを表現する。
 * ドメイン知識として、タグの使用状況を明示的に表現する。
 */
data class TagUsage(
    val name: String,
    val count: Int,
) {
    init {
        require(name.isNotBlank()) { "Tag name must not be blank" }
        require(count >= 0) { "Tag count must be non-negative" }
    }
}

