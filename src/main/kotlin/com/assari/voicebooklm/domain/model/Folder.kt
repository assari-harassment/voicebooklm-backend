package com.assari.voicebooklm.domain.model

import java.time.Instant
import java.util.UUID

/**
 * フォルダードメインモデル
 *
 * メモを分類するためのフォルダーを表現する。
 * 階層構造を持ち、親フォルダーへの参照を保持する。
 * イミュータブルな設計で、変更時は新しいインスタンスを返す。
 */
data class Folder(
    val id: UUID,
    val userId: UUID,
    val name: String,
    val parentId: UUID?,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
) {
    init {
        validate()
    }

    private fun validate() {
        require(name.isNotBlank()) { "Folder name must not be blank" }
        require(name.length <= MAX_NAME_LENGTH) {
            "Folder name must not exceed $MAX_NAME_LENGTH characters"
        }
    }

    /**
     * フォルダー名を変更した新しい Folder インスタンスを返す
     */
    fun rename(newName: String): Folder = copy(
        name = newName.trim(),
        updatedAt = Instant.now(),
    )

    /**
     * 親フォルダーを変更した新しい Folder インスタンスを返す
     */
    fun moveTo(newParentId: UUID?): Folder = copy(
        parentId = newParentId,
        updatedAt = Instant.now(),
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Folder) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    companion object {
        const val MAX_NAME_LENGTH = 50

        /**
         * 新規フォルダーを作成
         */
        fun create(
            id: UUID,
            userId: UUID,
            name: String,
            parentId: UUID? = null,
        ): Folder {
            val normalizedName = name.trim()
            return Folder(
                id = id,
                userId = userId,
                name = normalizedName,
                parentId = parentId,
            )
        }

        /**
         * 永続化されたデータから復元
         */
        fun restore(
            id: UUID,
            userId: UUID,
            name: String,
            parentId: UUID?,
            createdAt: Instant,
            updatedAt: Instant,
        ): Folder = Folder(
            id = id,
            userId = userId,
            name = name,
            parentId = parentId,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }
}


/**
 * フォルダーのフルパスを構築する
 *
 * @param folderMap フォルダーIDをキーとするフォルダーマップ
 * @return "/" 区切りのフルパス（例: "仕事/プロジェクトA/設計"）
 */
fun Folder.buildPath(folderMap: Map<UUID, Folder>): String {
    val pathSegments = mutableListOf(name)
    var currentParentId = parentId

    while (currentParentId != null) {
        val parent = folderMap[currentParentId] ?: break
        pathSegments.add(0, parent.name)
        currentParentId = parent.parentId
    }

    return pathSegments.joinToString("/")
}
