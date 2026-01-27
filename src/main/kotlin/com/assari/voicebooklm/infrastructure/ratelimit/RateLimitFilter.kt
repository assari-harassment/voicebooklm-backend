package com.assari.voicebooklm.infrastructure.ratelimit

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * レート制限フィルター
 *
 * 認証エンドポイント（/api/auth/）へのリクエストに対してレート制限を適用する。
 * IP アドレスベースでリクエスト数を制限し、超過時は 429 Too Many Requests を返す。
 */
@Component
class RateLimitFilter(
    private val rateLimitService: RateLimitService,
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

    companion object {
        private const val AUTH_PATH_PREFIX = "/api/auth/"
        private const val HEADER_X_FORWARDED_FOR = "X-Forwarded-For"
        private const val HEADER_RATE_LIMIT_REMAINING = "X-RateLimit-Remaining"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val requestPath = request.requestURI

        // 認証エンドポイント以外はスキップ
        if (!requestPath.startsWith(AUTH_PATH_PREFIX)) {
            filterChain.doFilter(request, response)
            return
        }

        val clientIp = getClientIp(request)

        if (!rateLimitService.tryConsumeForAuth(clientIp)) {
            sendRateLimitExceededResponse(response, clientIp)
            return
        }

        // 残りトークン数をレスポンスヘッダーに追加
        val remainingTokens = rateLimitService.getAvailableTokens(clientIp)
        response.setHeader(HEADER_RATE_LIMIT_REMAINING, remainingTokens.toString())

        filterChain.doFilter(request, response)
    }

    /**
     * クライアント IP アドレスを取得
     *
     * リバースプロキシ経由の場合は X-Forwarded-For ヘッダーを優先
     */
    private fun getClientIp(request: HttpServletRequest): String {
        val xForwardedFor = request.getHeader(HEADER_X_FORWARDED_FOR)
        return if (!xForwardedFor.isNullOrBlank()) {
            // カンマ区切りの場合は最初の IP を取得
            xForwardedFor.split(",").first().trim()
        } else {
            request.remoteAddr
        }
    }

    /**
     * レート制限超過レスポンスを送信
     */
    private fun sendRateLimitExceededResponse(response: HttpServletResponse, clientIp: String) {
        response.status = HttpStatus.TOO_MANY_REQUESTS.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"

        val errorResponse = mapOf(
            "error" to "リクエスト数の上限に達しました。しばらく待ってから再試行してください。",
            "code" to "RATE_LIMIT_EXCEEDED"
        )

        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }
}
