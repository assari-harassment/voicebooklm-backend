package com.assari.voicebooklm.infrastructure.security

import com.assari.voicebooklm.domain.model.User
import com.assari.voicebooklm.domain.repository.UserRepository
import com.github.f4b6a3.uuid.UuidCreator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Instant

/**
 * Firebase 認証フィルタ
 *
 * Authorization ヘッダーから Firebase ID Token を抽出し、検証する。
 * 有効なトークンの場合、SecurityContext に認証情報を設定する。
 * ユーザーが存在しない場合は自動作成（JIT Provisioning）を行う。
 */
@Component
class FirebaseAuthenticationFilter(
    private val userRepository: UserRepository
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(FirebaseAuthenticationFilter::class.java)

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }

    // 非同期ディスパッチでも必ずトークン検証を行う
    override fun shouldNotFilterAsyncDispatch(): Boolean = false

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val token = extractToken(request)

            if (token != null) {
                val decodedToken = FirebaseAuth.getInstance().verifyIdToken(token)
                val firebaseUid = decodedToken.uid
                val email = decodedToken.email

                if (!email.isNullOrBlank()) {
                    // ユーザーが存在しなければ自動作成（JIT Provisioning）
                    val user = userRepository.findByGoogleSub(firebaseUid)
                        ?: createUser(
                            firebaseUid = firebaseUid,
                            email = email,
                            name = decodedToken.name ?: email.substringBefore('@')
                        )

                    val authentication = UsernamePasswordAuthenticationToken(
                        user.id,
                        null,
                        listOf(SimpleGrantedAuthority("ROLE_USER"))
                    )
                    SecurityContextHolder.getContext().authentication = authentication
                    log.debug("Set Firebase authentication for user: ${user.id}")
                }
            }
        } catch (e: FirebaseAuthException) {
            log.debug("Firebase token validation failed: ${e.message}")
        } catch (e: Exception) {
            log.debug("Could not set user authentication: ${e.message}")
        }

        filterChain.doFilter(request, response)
    }

    /**
     * 新規ユーザーを作成する（JIT Provisioning）
     */
    private fun createUser(firebaseUid: String, email: String, name: String): User {
        val user = User(
            id = UuidCreator.getTimeOrderedEpoch(),
            googleSub = firebaseUid,
            email = email,
            name = name,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        log.info("Created new user via JIT provisioning: $email")
        return userRepository.save(user)
    }

    /**
     * リクエストからトークンを抽出する
     */
    private fun extractToken(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader(AUTHORIZATION_HEADER)
        return if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            bearerToken.substring(BEARER_PREFIX.length)
        } else {
            null
        }
    }
}
