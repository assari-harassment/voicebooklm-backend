package com.assari.voicebooklm.presentation.controller.auth

import com.assari.voicebooklm.domain.gateway.OAuthClient
import com.assari.voicebooklm.domain.gateway.TokenProvider
import com.assari.voicebooklm.domain.repository.MemoRepository
import com.assari.voicebooklm.domain.repository.RefreshTokenRepository
import com.assari.voicebooklm.domain.repository.UserRepository
import com.assari.voicebooklm.usecase.auth.DeleteAccountCommand
import com.assari.voicebooklm.usecase.auth.DeleteAccountInteractor
import com.assari.voicebooklm.usecase.auth.DeleteAccountUseCase
import com.assari.voicebooklm.usecase.auth.GetCurrentUserCommand
import com.assari.voicebooklm.usecase.auth.GetCurrentUserInteractor
import com.assari.voicebooklm.usecase.auth.GetCurrentUserUseCase
import com.assari.voicebooklm.usecase.auth.LoginCommand
import com.assari.voicebooklm.usecase.auth.LoginInteractor
import com.assari.voicebooklm.usecase.auth.LoginUseCase
import com.assari.voicebooklm.usecase.auth.LogoutCommand
import com.assari.voicebooklm.usecase.auth.LogoutInteractor
import com.assari.voicebooklm.usecase.auth.LogoutUseCase
import com.assari.voicebooklm.usecase.auth.RefreshTokenCommand
import com.assari.voicebooklm.usecase.auth.RefreshTokenInteractor
import com.assari.voicebooklm.usecase.auth.RefreshTokenUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * 認証 REST API コントローラー
 *
 * Google OAuth 認証、トークンリフレッシュ、ログアウト、アカウント削除を処理する。
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "認証関連 API")
class AuthController(
    // ユースケースを Bean 登録せず、このコントローラで組み立てて依存を閉じる
    // （オニオンアーキテクチャのコンポジションルートをここに置く）
    private val oAuthClient: OAuthClient,
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val tokenProvider: TokenProvider,
    private val memoRepository: MemoRepository,
) {
    // Auth 系ユースケースをここで明示的に生成し、外部からは Spring に依存しない形で扱う。
    // Bean 化しない代わりに、依存をコンストラクタで受けて手動 new する。
    private val loginUseCase: LoginUseCase =
        LoginInteractor(
            oAuthClient = oAuthClient,
            userRepository = userRepository,
            refreshTokenRepository = refreshTokenRepository,
            tokenProvider = tokenProvider,
        )
    private val refreshTokenUseCase: RefreshTokenUseCase =
        RefreshTokenInteractor(
            refreshTokenRepository = refreshTokenRepository,
            userRepository = userRepository,
            tokenProvider = tokenProvider,
        )
    // リフレッシュトークンの破棄のみを行うシンプルなユースケース
    private val logoutUseCase: LogoutUseCase = LogoutInteractor(refreshTokenRepository)
    private val deleteAccountUseCase: DeleteAccountUseCase =
        DeleteAccountInteractor(
            userRepository = userRepository,
            memoRepository = memoRepository,
            refreshTokenRepository = refreshTokenRepository,
        )
    private val getCurrentUserUseCase: GetCurrentUserUseCase =
        GetCurrentUserInteractor(userRepository)
    /** Google OAuth でログイン */
    @Operation(
            summary = "Google OAuth ログイン",
            description = "Google ID トークンを検証し、JWT トークンペアを発行します。新規ユーザーの場合はアカウントを作成します。"
    )
    @ApiResponses(
            ApiResponse(responseCode = "200", description = "ログイン成功"),
            ApiResponse(
                    responseCode = "401",
                    description = "認証失敗",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
    )
    @PostMapping("/google")
    suspend fun loginWithGoogle(
            @Valid @RequestBody request: GoogleAuthRequest
    ): ResponseEntity<TokenResponse> {
        val result = loginUseCase.execute(LoginCommand(request.idToken))
        return ResponseEntity.ok(
                TokenResponse(accessToken = result.accessToken, refreshToken = result.refreshToken)
        )
    }

    /** トークンリフレッシュ */
    @Operation(summary = "トークンリフレッシュ", description = "リフレッシュトークンを使用して新しいトークンペアを発行します（トークンローテーション）。")
    @ApiResponses(
            ApiResponse(responseCode = "200", description = "リフレッシュ成功"),
            ApiResponse(
                    responseCode = "401",
                    description = "リフレッシュトークンが無効または期限切れ",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
    )
    @PostMapping("/refresh")
    fun refreshToken(
            @Valid @RequestBody request: RefreshTokenRequest
    ): ResponseEntity<TokenResponse> {
        val result = refreshTokenUseCase.execute(RefreshTokenCommand(request.refreshToken))
        return ResponseEntity.ok(
                TokenResponse(accessToken = result.accessToken, refreshToken = result.refreshToken)
        )
    }

    /** ログアウト */
    @Operation(summary = "ログアウト", description = "リフレッシュトークンを無効化してログアウトします。")
    @ApiResponses(
            ApiResponse(responseCode = "204", description = "ログアウト成功"),
            ApiResponse(
                    responseCode = "400",
                    description = "リクエスト形式が不正",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
    )
    @PostMapping("/logout")
    fun logout(@Valid @RequestBody request: LogoutRequest): ResponseEntity<Void> {
        logoutUseCase.execute(LogoutCommand(request.refreshToken))
        return ResponseEntity.noContent().build()
    }

    /** アカウント削除 */
    @Operation(summary = "アカウント削除", description = "ユーザーアカウントとすべての関連データを完全に削除します。")
    @ApiResponses(
            ApiResponse(responseCode = "204", description = "削除成功"),
            ApiResponse(
                    responseCode = "401",
                    description = "認証失敗",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                    responseCode = "404",
                    description = "ユーザーが見つからない",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
    )
    @DeleteMapping("/account")
    fun deleteAccount(@AuthenticationPrincipal userId: UUID?): ResponseEntity<Void> {
        userId ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        deleteAccountUseCase.execute(DeleteAccountCommand(userId))
        return ResponseEntity.noContent().build()
    }

    /** 現在のユーザー情報取得 */
    @Operation(summary = "現在のユーザー情報取得", description = "認証済みユーザーの情報を取得します。")
    @ApiResponses(
            ApiResponse(responseCode = "200", description = "取得成功"),
            ApiResponse(
                    responseCode = "401",
                    description = "認証失敗",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                    responseCode = "404",
                    description = "ユーザーが見つからない",
                    content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
    )
    @GetMapping("/me")
    fun getCurrentUser(@AuthenticationPrincipal userId: UUID?): ResponseEntity<UserResponse> {
        userId ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val userInfo = getCurrentUserUseCase.execute(GetCurrentUserCommand(userId))
        return ResponseEntity.ok(
                UserResponse(id = userInfo.id, email = userInfo.email, name = userInfo.name)
        )
    }
}
