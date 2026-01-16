package com.assari.voicebooklm.presentation.controller.dev

import com.assari.voicebooklm.domain.gateway.TokenProvider
import com.assari.voicebooklm.domain.repository.UserRepository
import com.assari.voicebooklm.usecase.dev.DevSeedUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

/**
 * 開発用 API
 *
 * dev profile でのみ有効。
 */
@RestController
@RequestMapping("/api/dev")
@Profile("dev")
@Tag(name = "Dev", description = "開発用 API（dev 環境のみ有効）")
class DevSeedController(
    private val devSeedUseCase: DevSeedUseCase,
    private val userRepository: UserRepository,
    private val tokenProvider: TokenProvider,
) {
    @PostMapping("/token")
    @Operation(
        summary = "開発用トークン取得",
        description = """
            指定したメールアドレスのユーザーのアクセストークンを取得する。
            OAuth不要で、Swagger UIからすぐにAPIテストができる。

            **注意**: dev 環境でのみ有効。本番環境では使用不可。
        """,
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "トークン取得成功",
                content = [Content(schema = Schema(implementation = DevTokenResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "ユーザーが見つからない",
            ),
        ],
    )
    fun getToken(
        @Parameter(description = "ユーザーのメールアドレス", required = true)
        @RequestParam email: String,
    ): ResponseEntity<DevTokenResponse> {
        val user = userRepository.findByEmail(email)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        val accessToken = tokenProvider.generateAccessToken(user.id, user.email)

        return ResponseEntity.ok(
            DevTokenResponse(
                accessToken = accessToken,
                userId = user.id.toString(),
                email = user.email,
                message = "Swagger UI の Authorize で「Bearer $accessToken」を設定してください",
            )
        )
    }

    @PostMapping("/seed")
    @Operation(
        summary = "テストデータ作成",
        description = """
            認証ユーザー用にテスト用のフォルダーとメモを作成する。
            データは seed-data.yml から読み込まれる。

            冪等性を持ち、既にフォルダーが存在する場合はスキップする。
        """,
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "作成成功（またはスキップ）",
                content = [Content(schema = Schema(implementation = DevSeedResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "認証エラー",
            ),
        ],
    )
    suspend fun seed(
        @AuthenticationPrincipal userId: UUID?,
    ): ResponseEntity<DevSeedResponse> {
        // 認証チェック
        if (userId == null) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "認証が必要です")
        }

        val result = devSeedUseCase.execute(userId)

        return ResponseEntity.ok(
            DevSeedResponse(
                foldersCreated = result.foldersCreated,
                memosCreated = result.memosCreated,
                skipped = result.skipped,
                message = result.message,
            )
        )
    }
}

@Schema(description = "開発用トークン取得レスポンス")
data class DevTokenResponse(
    @Schema(description = "アクセストークン（JWT）")
    val accessToken: String,

    @Schema(description = "ユーザーID")
    val userId: String,

    @Schema(description = "メールアドレス")
    val email: String,

    @Schema(description = "使い方の説明")
    val message: String,
)

@Schema(description = "開発用シードデータ作成レスポンス")
data class DevSeedResponse(
    @Schema(description = "作成されたフォルダー数")
    val foldersCreated: Int,

    @Schema(description = "作成されたメモ数")
    val memosCreated: Int,

    @Schema(description = "スキップされたかどうか（既存データがある場合）")
    val skipped: Boolean,

    @Schema(description = "結果メッセージ")
    val message: String,
)
