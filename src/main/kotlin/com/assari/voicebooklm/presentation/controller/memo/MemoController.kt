package com.assari.voicebooklm.presentation.controller.memo

import com.assari.voicebooklm.presentation.controller.auth.ErrorResponse
import com.assari.voicebooklm.usecase.memo.ListMemosInput
import com.assari.voicebooklm.usecase.memo.ListMemosUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * メモ一覧取得 API
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Memo", description = "メモ一覧 API")
class MemoController(
    private val listMemosUseCase: ListMemosUseCase,
) {
    @GetMapping("/memos")
    @Operation(
        summary = "メモ一覧取得",
        description = "検索は未対応。認証ユーザーのメモを新しい順で取得する。",
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
    ): ResponseEntity<ListMemosResponse> {
        userId ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val result = listMemosUseCase.execute(ListMemosInput(userId))
        return ResponseEntity.ok(ListMemosResponse.from(result))
    }
}
