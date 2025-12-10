package com.assari.voicebooklm.infrastructure.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

/**
 * JWT トークンプロバイダー
 *
 * JWT アクセストークン・リフレッシュトークンの生成・検証を行う。
 * HS256 アルゴリズムを使用。
 */
@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}")
    private val secret: String,

    @Value("\${jwt.access-token-expiration}")
    private val accessTokenExpiration: Long,

    @Value("\${jwt.refresh-token-expiration}")
    private val refreshTokenExpiration: Long
) {
    private val logger = LoggerFactory.getLogger(JwtTokenProvider::class.java)

    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    companion object {
        private const val CLAIM_USER_ID = "userId"
        private const val CLAIM_EMAIL = "email"
        private const val TOKEN_TYPE = "tokenType"
        private const val TOKEN_TYPE_ACCESS = "access"
        private const val TOKEN_TYPE_REFRESH = "refresh"
    }

    /**
     * アクセストークンを生成する
     *
     * @param userId ユーザー ID
     * @param email メールアドレス
     * @return JWT アクセストークン
     */
    fun generateAccessToken(userId: UUID, email: String): String {
        val now = Date()
        val expiry = Date(now.time + accessTokenExpiration)

        return Jwts.builder()
            .subject(userId.toString())
            .claim(CLAIM_USER_ID, userId.toString())
            .claim(CLAIM_EMAIL, email)
            .claim(TOKEN_TYPE, TOKEN_TYPE_ACCESS)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key)
            .compact()
    }

    /**
     * リフレッシュトークンを生成する
     *
     * @param userId ユーザー ID
     * @return JWT リフレッシュトークン
     */
    fun generateRefreshToken(userId: UUID): String {
        val now = Date()
        val expiry = Date(now.time + refreshTokenExpiration)

        return Jwts.builder()
            .subject(userId.toString())
            .claim(CLAIM_USER_ID, userId.toString())
            .claim(TOKEN_TYPE, TOKEN_TYPE_REFRESH)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key)
            .compact()
    }

    /**
     * トークンを検証する
     *
     * @param token JWT トークン
     * @return トークンが有効な場合は true
     */
    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
            true
        } catch (e: Exception) {
            logger.debug("JWT validation failed: ${e.message}")
            false
        }
    }

    /**
     * トークンからユーザー ID を取得する
     *
     * @param token JWT トークン
     * @return ユーザー ID（無効なトークンの場合は null）
     */
    fun getUserIdFromToken(token: String): UUID? {
        return try {
            val claims = getClaims(token)
            UUID.fromString(claims[CLAIM_USER_ID] as String)
        } catch (e: Exception) {
            logger.debug("Failed to extract userId from token: ${e.message}")
            null
        }
    }

    /**
     * トークンからメールアドレスを取得する
     *
     * @param token JWT トークン
     * @return メールアドレス（無効なトークンの場合は null）
     */
    fun getEmailFromToken(token: String): String? {
        return try {
            val claims = getClaims(token)
            claims[CLAIM_EMAIL] as? String
        } catch (e: Exception) {
            logger.debug("Failed to extract email from token: ${e.message}")
            null
        }
    }

    /**
     * トークンからクレームを取得する
     */
    private fun getClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
