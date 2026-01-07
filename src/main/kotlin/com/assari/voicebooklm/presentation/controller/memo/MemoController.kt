package com.assari.voicebooklm.presentation.controller.memo

import com.assari.voicebooklm.presentation.controller.auth.ErrorResponse
import com.assari.voicebooklm.usecase.memo.DeleteMemoInput
import com.assari.voicebooklm.usecase.memo.DeleteMemoUseCase
import com.assari.voicebooklm.usecase.memo.GetMemoInput
import com.assari.voicebooklm.usecase.memo.GetMemoUseCase
import com.assari.voicebooklm.usecase.memo.ListMemosInput
import com.assari.voicebooklm.usecase.memo.ListMemosUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/**
 * メモ API
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Memo", description = "メモ API")
class MemoController(
    private val listMemosUseCase: ListMemosUseCase,
    private val getMemoUseCase: GetMemoUseCase,
    private val deleteMemoUseCase: DeleteMemoUseCase,
) {
    @GetMapping("/memos")
    @Operation(
        summary = "メモ一覧取得",
        description = "認証ユーザーのメモを取得する。フォルダーによるフィルタリングが可能。",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "取得成功",
                content = [Content(schema = Schema(implementation = ListMemosResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "認証エラー",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    suspend fun listMemos(
        @AuthenticationPrincipal userId: UUID?,
        @Parameter(description = "フォルダーIDでフィルタリング")
        @RequestParam(required = false) folderId: UUID?,
        @Parameter(description = "true の場合、子孫フォルダーのメモも含める")
        @RequestParam(required = false, defaultValue = "false") includeDescendants: Boolean,
        @Parameter(description = "true の場合、未分類メモのみ取得")
        @RequestParam(required = false, defaultValue = "false") uncategorizedOnly: Boolean,
    ): ResponseEntity<ListMemosResponse> {
        // 認証チェック
        if (userId == null) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "認証が必要です")
        }

        val result = listMemosUseCase.execute(
            ListMemosInput(
                userId = userId,
                folderId = folderId,
                includeDescendants = includeDescendants,
                uncategorizedOnly = uncategorizedOnly,
            )
        )
        return ResponseEntity.ok(ListMemosResponse.from(result))
    }

    @GetMapping("/memos/{id}")
    @Operation(
        summary = "メモ詳細取得",
        description = "指定されたIDのメモの詳細情報を取得します。セキュリティ上、権限のないメモも404として返します。",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "取得成功",
                content = [Content(schema = Schema(implementation = MemoDetailResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "認証失敗",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "メモが見つからない（存在しない、削除済み、または権限なし）",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    suspend fun getMemo(
        @PathVariable id: UUID,
        @AuthenticationPrincipal userId: UUID?,
    ): ResponseEntity<MemoDetailResponse> {
        // 認証チェック
        if (userId == null) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "認証が必要です")
        }

        // ユースケース実行
        val result = getMemoUseCase.execute(GetMemoInput(id, userId))

        // レスポンス返却
        return ResponseEntity.ok(MemoDetailResponse.from(result))
    }

    @DeleteMapping("/memos/{id}")
    @Operation(
        summary = "メモ削除",
        description = "指定されたメモを削除します。",
        responses = [
            ApiResponse(
                responseCode = "204",
                description = "削除成功",
            ),
            ApiResponse(
                responseCode = "401",
                description = "認証失敗",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "アクセス権限なし",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "メモが見つからない",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    suspend fun deleteMemo(
        @PathVariable id: UUID,
        @AuthenticationPrincipal userId: UUID?,
    ): ResponseEntity<Void> {
        // 認証チェック
        if (userId == null) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "認証が必要です")
        }

        // ユースケース実行
        deleteMemoUseCase.execute(DeleteMemoInput(id, userId))

        // 204 No Content を返却
        return ResponseEntity.noContent().build()
    }
}
