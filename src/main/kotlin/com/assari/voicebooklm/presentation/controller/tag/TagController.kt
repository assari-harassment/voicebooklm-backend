package com.assari.voicebooklm.presentation.controller.tag

import com.assari.voicebooklm.domain.model.SortOrder
import com.assari.voicebooklm.domain.model.TagSortField
import com.assari.voicebooklm.presentation.controller.auth.ErrorResponse
import com.assari.voicebooklm.usecase.tag.ListTagsInput
import com.assari.voicebooklm.usecase.tag.ListTagsUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

/**
 * タグ API
 */
@RestController
@RequestMapping("/api/tags")
@Tag(name = "Tag", description = "タグ API")
class TagController(
    private val listTagsUseCase: ListTagsUseCase,
) {
    @GetMapping
    @Operation(
        summary = "タグ一覧取得",
        description = """
            認証ユーザーが使用している全タグを取得する。
            ソート順と件数制限が指定可能。

            人気タグを取得する場合: sort=usage_count&order=desc&limit=10
        """,
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "取得成功",
                content = [Content(schema = Schema(implementation = TagsResponse::class))],
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
        @Parameter(description = "ソート項目（name: 名前順, usage_count: 使用回数順）")
        @RequestParam(required = false, defaultValue = "name") sort: String,
        @Parameter(description = "ソート順序（asc, desc）")
        @RequestParam(required = false, defaultValue = "asc") order: String,
        @Parameter(description = "取得件数制限")
        @RequestParam(required = false) limit: Int?,
    ): ResponseEntity<TagsResponse> {
        if (userId == null) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "認証が必要です")
        }

        if (limit != null && limit <= 0) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "limitは1以上の値を指定してください")
        }

        val sortField = when (sort.lowercase()) {
            "name" -> TagSortField.NAME
            "usage_count" -> TagSortField.USAGE_COUNT
            else -> TagSortField.NAME
        }

        val sortOrder = when (order.lowercase()) {
            "asc" -> SortOrder.ASC
            "desc" -> SortOrder.DESC
            else -> SortOrder.ASC
        }

        val result = listTagsUseCase.execute(
            ListTagsInput(
                userId = userId,
                sort = sortField,
                order = sortOrder,
                limit = limit,
            )
        )

        return ResponseEntity.ok(TagsResponse(tags = result.tags))
    }
}
