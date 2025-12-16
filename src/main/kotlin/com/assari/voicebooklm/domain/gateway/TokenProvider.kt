package com.assari.voicebooklm.domain.gateway

import java.util.UUID

/**
 * アクセス/リフレッシュトークンを発行するためのポート。
 * インフラ層の具体実装（例: JWT）に依存しないための抽象化。
 */
interface TokenProvider {
    val refreshTokenExpiration: Long

    fun generateAccessToken(userId: UUID, email: String): String

    fun generateRefreshToken(userId: UUID): String
}
