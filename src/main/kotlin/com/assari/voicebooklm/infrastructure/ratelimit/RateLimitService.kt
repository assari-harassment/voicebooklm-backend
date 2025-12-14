package com.assari.voicebooklm.infrastructure.ratelimit

import com.assari.voicebooklm.config.RateLimitProperties
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

/**
 * レート制限サービス
 *
 * IP アドレスごとにトークンバケットを管理し、リクエスト数を制限する。
 * Bucket4j ライブラリを使用したトークンバケットアルゴリズムを実装。
 */
@Service
class RateLimitService(
    private val rateLimitProperties: RateLimitProperties
) {
    private val logger = LoggerFactory.getLogger(RateLimitService::class.java)

    /**
     * IP アドレスごとのバケットキャッシュ
     */
    private val buckets = ConcurrentHashMap<String, Bucket>()

    /**
     * 認証エンドポイント用のレート制限チェック
     *
     * @param clientIp クライアント IP アドレス
     * @return リクエストが許可された場合は true
     */
    fun tryConsumeForAuth(clientIp: String): Boolean {
        if (!rateLimitProperties.enabled) {
            return true
        }

        val bucket = buckets.computeIfAbsent(clientIp) { createAuthBucket() }
        val consumed = bucket.tryConsume(1)

        if (!consumed) {
            logger.warn("Rate limit exceeded for IP: {}", clientIp)
        }

        return consumed
    }

    /**
     * 指定 IP アドレスの残りトークン数を取得
     *
     * @param clientIp クライアント IP アドレス
     * @return 残りトークン数
     */
    fun getAvailableTokens(clientIp: String): Long {
        val bucket = buckets[clientIp] ?: return rateLimitProperties.auth.capacity
        return bucket.availableTokens
    }

    /**
     * 認証エンドポイント用バケットを作成
     */
    private fun createAuthBucket(): Bucket {
        val config = rateLimitProperties.auth
        val bandwidth = Bandwidth.builder()
            .capacity(config.capacity)
            .refillGreedy(config.refillTokens, config.getRefillDuration())
            .build()

        return Bucket.builder()
            .addLimit(bandwidth)
            .build()
    }

    /**
     * キャッシュをクリア（テスト用）
     */
    fun clearCache() {
        buckets.clear()
    }
}
