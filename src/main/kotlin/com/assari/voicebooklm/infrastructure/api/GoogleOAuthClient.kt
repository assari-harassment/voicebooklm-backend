package com.assari.voicebooklm.infrastructure.api

import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

/**
 * Google OAuth ユーザー情報
 */
data class GoogleUserInfo(
    val googleSub: String,
    val email: String,
    val name: String,
    val picture: String?
)

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
 * Google ID トークンの検証とユーザー情報の取得を行う。
 * モバイルアプリからの ID トークンを検証する用途を想定。
 */
@Component
class GoogleOAuthClient(
    @Value("\${spring.security.oauth2.client.registration.google.client-id}")
    private val clientId: String,
    private val webClientBuilder: WebClient.Builder
) {
    private val logger = LoggerFactory.getLogger(GoogleOAuthClient::class.java)

    private val webClient: WebClient =
        webClientBuilder
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
    fun verifyIdTokenAndGetUserInfo(idToken: String): GoogleUserInfo? {
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

            GoogleUserInfo(
                googleSub = tokenInfo.sub,
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
