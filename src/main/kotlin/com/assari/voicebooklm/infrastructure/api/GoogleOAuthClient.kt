package com.assari.voicebooklm.infrastructure.api

import com.assari.voicebooklm.domain.gateway.OAuthClient
import com.assari.voicebooklm.domain.model.OAuthUserInfo
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import reactor.netty.http.client.HttpClient
import java.time.Duration

/**
 * Google Token Info レスポンス
 */
data class GoogleTokenInfo(
    val aud: String,
    val sub: String,
    val email: String,
    @JsonProperty("email_verified")
    val emailVerified: Boolean,
    val name: String?,
    val picture: String?,
    @JsonProperty("given_name")
    val givenName: String?,
    @JsonProperty("family_name")
    val familyName: String?
)

/**
 * Google OAuth クライアント
 *
 * OAuthClient インターフェースの Google 実装。
 * Google ID トークンの検証とユーザー情報の取得を行う。
 * モバイルアプリからの ID トークンを検証する用途を想定。
 */
@Component
class GoogleOAuthClient(
    @Value("\${spring.security.oauth2.client.registration.google.client-id}")
    private val clientId: String,
) : OAuthClient {

    private val logger = LoggerFactory.getLogger(GoogleOAuthClient::class.java)

    // WebClient を Bean にせず、ここで必要な設定（タイムアウト含む）を付けて組み立てる
    private val webClient: WebClient =
        WebClient.builder()
            .clientConnector(
                ReactorClientHttpConnector(
                    HttpClient.create()
                        .responseTimeout(Duration.ofSeconds(60)),
                ),
            )
            .baseUrl("https://oauth2.googleapis.com")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build()

    companion object {
        private const val TOKEN_INFO_ENDPOINT = "/tokeninfo"
    }

    /**
     * Google ID トークンを検証し、ユーザー情報を取得する
     *
     * @param idToken Google ID トークン
     * @return ユーザー情報（検証失敗時は null）
     */
    override fun verifyIdTokenAndGetUserInfo(idToken: String): OAuthUserInfo? {
        return try {
            val tokenInfo = webClient.get()
                .uri { uriBuilder ->
                    uriBuilder
                        .path(TOKEN_INFO_ENDPOINT)
                        .queryParam("id_token", idToken)
                        .build()
                }
                .retrieve()
                .bodyToMono(GoogleTokenInfo::class.java)
                .block()

            if (tokenInfo == null) {
                logger.warn("Failed to verify Google ID token: no response")
                return null
            }

            if (tokenInfo.aud != clientId) {
                logger.warn("Google ID token audience mismatch: expected=$clientId, actual=${tokenInfo.aud}")
                return null
            }

            if (!tokenInfo.emailVerified) {
                logger.warn("Google email not verified for user: ${tokenInfo.email}")
                return null
            }

            OAuthUserInfo(
                providerId = tokenInfo.sub,
                email = tokenInfo.email,
                name = tokenInfo.name ?: tokenInfo.givenName ?: "Unknown",
                picture = tokenInfo.picture
            )
        } catch (e: Exception) {
            logger.error("Failed to verify Google ID token: ${e.message}", e)
            null
        }
    }
}
