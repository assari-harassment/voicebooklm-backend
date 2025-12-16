package com.assari.voicebooklm.config

import com.assari.voicebooklm.domain.repository.MemoRepository
import com.assari.voicebooklm.domain.repository.RefreshTokenRepository
import com.assari.voicebooklm.domain.repository.UserRepository
import com.assari.voicebooklm.domain.gateway.OAuthClient
import com.assari.voicebooklm.domain.gateway.TokenProvider
import com.assari.voicebooklm.usecase.auth.DeleteAccountInteractor
import com.assari.voicebooklm.usecase.auth.DeleteAccountUseCase
import com.assari.voicebooklm.usecase.auth.GetCurrentUserInteractor
import com.assari.voicebooklm.usecase.auth.GetCurrentUserUseCase
import com.assari.voicebooklm.usecase.auth.LoginInteractor
import com.assari.voicebooklm.usecase.auth.LoginUseCase
import com.assari.voicebooklm.usecase.auth.LogoutInteractor
import com.assari.voicebooklm.usecase.auth.LogoutUseCase
import com.assari.voicebooklm.usecase.auth.RefreshTokenInteractor
import com.assari.voicebooklm.usecase.auth.RefreshTokenUseCase
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
 * Spring に依存するのはこの構成ルートのみとし、ユースケース本体はプレーンなクラスで組み立てる。
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

    @Bean
    fun loginUseCase(
        oAuthClient: OAuthClient,
        userRepository: UserRepository,
        refreshTokenRepository: RefreshTokenRepository,
        tokenProvider: TokenProvider,
    ): LoginUseCase =
        LoginInteractor(
            oAuthClient = oAuthClient,
            userRepository = userRepository,
            refreshTokenRepository = refreshTokenRepository,
            tokenProvider = tokenProvider,
        )

    @Bean
    fun refreshTokenUseCase(
        refreshTokenRepository: RefreshTokenRepository,
        userRepository: UserRepository,
        tokenProvider: TokenProvider,
    ): RefreshTokenUseCase =
        RefreshTokenInteractor(
            refreshTokenRepository = refreshTokenRepository,
            userRepository = userRepository,
            tokenProvider = tokenProvider,
        )

    @Bean
    fun logoutUseCase(
        refreshTokenRepository: RefreshTokenRepository,
    ): LogoutUseCase =
        LogoutInteractor(refreshTokenRepository)

    @Bean
    fun deleteAccountUseCase(
        userRepository: UserRepository,
        memoRepository: MemoRepository,
        refreshTokenRepository: RefreshTokenRepository,
    ): DeleteAccountUseCase =
        DeleteAccountInteractor(
            userRepository = userRepository,
            memoRepository = memoRepository,
            refreshTokenRepository = refreshTokenRepository,
        )

    @Bean
    fun getCurrentUserUseCase(
        userRepository: UserRepository,
    ): GetCurrentUserUseCase =
        GetCurrentUserInteractor(userRepository)
}
