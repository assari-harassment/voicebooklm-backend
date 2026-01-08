package com.assari.voicebooklm.presentation.controller.tag

import com.assari.voicebooklm.presentation.controller.auth.ErrorResponse
import com.assari.voicebooklm.usecase.tag.ListTagsInput
import com.assari.voicebooklm.usecase.tag.ListTagsUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * タグ API
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Tag", description = "タグ API")
class TagController(
    private val listTagsUseCase: ListTagsUseCase,
) {
    @GetMapping("/tags")
    @Operation(
        summary = "タグ一覧取得",
        description = "認証ユーザーのメモで使用されているタグを使用回数の降順で取得する。",
        parameters = [
            Parameter(
                name = "limit",
                description = "取得件数上限（指定しない場合は全件取得）",
                required = false,
                schema = Schema(type = "integer", minimum = "1"),
            ),
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "取得成功",
                content = [Content(schema = Schema(implementation = ListTagsResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "認証エラー",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    suspend fun listTags(
        @AuthenticationPrincipal userId: UUID?,
        @RequestParam(required = false) limit: Int?,
    ): ResponseEntity<ListTagsResponse> {
        userId ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val result = listTagsUseCase.execute(ListTagsInput(userId = userId, limit = limit))
        return ResponseEntity.ok(ListTagsResponse.from(result))
    }
}
