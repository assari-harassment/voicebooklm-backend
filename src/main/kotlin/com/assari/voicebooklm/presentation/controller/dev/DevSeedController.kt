package com.assari.voicebooklm.presentation.controller.dev

import com.assari.voicebooklm.usecase.dev.DevSeedUseCase
import io.swagger.v3.oas.annotations.Operation
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
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * 開発用シードデータ API
 *
 * dev profile でのみ有効。
 * 認証済みユーザー用にテストデータを作成する。
 */
@RestController
@RequestMapping("/api/dev")
@Profile("dev")
@Tag(name = "Dev", description = "開発用 API（dev 環境のみ有効）")
class DevSeedController(
    private val devSeedUseCase: DevSeedUseCase,
) {
    @PostMapping("/seed")
    @Operation(
        summary = "テストデータ作成",
        description = """
            認証ユーザー用にテスト用のフォルダーとメモを作成する。

            作成されるデータ:
            - フォルダー: 仕事/会議, 学習/読書, プロジェクト/アプリ開発
            - メモ: 各フォルダーに1件ずつ（計3件）

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
        userId ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

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
