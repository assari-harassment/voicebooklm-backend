package com.assari.voicebooklm.config

import com.assari.voicebooklm.domain.repository.MemoRepository
import com.assari.voicebooklm.usecase.memo.CreateMemoInteractor
import com.assari.voicebooklm.usecase.memo.CreateMemoUseCase
import com.assari.voicebooklm.usecase.memo.client.AiMemoFormatter
import com.assari.voicebooklm.usecase.memo.client.SpeechTranscriber
import com.assari.voicebooklm.usecase.support.ExecutionTimer
import com.assari.voicebooklm.usecase.support.MonotonicExecutionTimer
import kotlin.time.TimeSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * アプリケーションサービスの DI 設定。
 */
@Configuration
class ApplicationServiceConfig {

    @Bean
    fun executionTimer(): ExecutionTimer = MonotonicExecutionTimer()

    @Bean
    fun createMemoUseCase(
        memoRepository: MemoRepository,
        speechTranscriber: SpeechTranscriber,
        aiMemoFormatter: AiMemoFormatter,
        executionTimer: ExecutionTimer,
    ): CreateMemoUseCase =
        CreateMemoInteractor(
            memoRepository = memoRepository,
            speechTranscriber = speechTranscriber,
            aiMemoFormatter = aiMemoFormatter,
            executionTimer = executionTimer,
            timeSource = TimeSource.Monotonic,
        )
}
