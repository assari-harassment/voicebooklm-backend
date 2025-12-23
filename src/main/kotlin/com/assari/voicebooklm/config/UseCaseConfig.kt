package com.assari.voicebooklm.config

import com.assari.voicebooklm.domain.repository.VoiceMemoRepository
import com.assari.voicebooklm.usecase.memo.GetTagsUseCase
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * ユースケースの DI 設定
 */
@Configuration
class UseCaseConfig {

    /**
     * タグ一覧取得ユースケース
     */
    @Bean
    fun getTagsUseCase(
        voiceMemoRepository: VoiceMemoRepository,
    ): GetTagsUseCase =
        GetTagsUseCase(voiceMemoRepository)
}




