package com.assari.voicebooklm.domain.repository

import com.assari.voicebooklm.domain.model.User
import java.util.UUID

/**
 * ユーザーリポジトリインターフェース
 *
 * Domain Layer で定義されるリポジトリインターフェース。
 * 実装は Infrastructure Layer で行う。
 */
interface UserRepository {
    /**
     * ユーザーを保存する
     */
    fun save(user: User): User

    /**
     * ID でユーザーを取得する
     */
    fun findById(id: UUID): User?

    /**
     * メールアドレスでユーザーを取得する
     */
    fun findByEmail(email: String): User?

    /**
     * Google Sub でユーザーを取得する
     */
    fun findByGoogleSub(googleSub: String): User?

    /**
     * ユーザーを削除する
     */
    fun deleteById(id: UUID)
}
