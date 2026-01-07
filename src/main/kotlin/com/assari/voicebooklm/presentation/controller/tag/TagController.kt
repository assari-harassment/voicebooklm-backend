package com.assari.voicebooklm.presentation.controller.tag

import com.assari.voicebooklm.presentation.controller.auth.ErrorResponse
import com.assari.voicebooklm.usecase.memo.GetTagsInput
import com.assari.voicebooklm.usecase.memo.GetTagsUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * タグ関連APIコントローラー
 */
@RestController
@RequestMapping("/api/tags")
@Tag(name = "Tags", description = "タグ関連 API")
class TagController(
    private val getTagsUseCase: GetTagsUseCase,
) {

    /**
     * タグ一覧取得
     *
     * 認証済みユーザーが所有するメモのタグ一覧を取得します。
     * 使用頻度の高い順にソートされます。
     */
    @GetMapping
    @Operation(
        summary = "タグ一覧取得",
        description = "認証済みユーザーが所有するメモのタグ一覧を取得します。使用頻度の高い順にソートされます。"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "取得成功"),
        ApiResponse(
            responseCode = "401",
            description = "認証失敗",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    suspend fun getTags(
        @AuthenticationPrincipal userId: UUID?
    ): ResponseEntity<TagResponse> {
        // 防御的プログラミング: SecurityConfigで認証必須だが、
        // フィルターの実装変更やエッジケースに備えてnullチェックを維持
        // 他のコントローラーと一貫性を保つため、UUID?のままとする
        userId ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val result = getTagsUseCase.execute(GetTagsInput(userId))
        val response = TagResponse.from(result)

        return ResponseEntity.ok(response)
    }
}

