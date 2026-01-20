package com.assari.voicebooklm.infrastructure.api.speech

import com.assari.voicebooklm.config.GoogleCloudProperties
import com.assari.voicebooklm.domain.gateway.StreamingSpeechTranscriber
import com.assari.voicebooklm.domain.gateway.StreamingTranscriptionConfig
import com.assari.voicebooklm.domain.gateway.StreamingTranscriptionSession
import com.assari.voicebooklm.domain.model.TranscriptionResult
import com.google.api.gax.rpc.ApiStreamObserver
import com.google.cloud.speech.v1.RecognitionConfig
import com.google.cloud.speech.v1.SpeechClient
import com.google.cloud.speech.v1.StreamingRecognitionConfig
import com.google.cloud.speech.v1.StreamingRecognizeRequest
import com.google.cloud.speech.v1.StreamingRecognizeResponse
import com.google.protobuf.ByteString
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Google Cloud Speech-to-Text Streaming API 実装
 *
 * StreamingRecognize API を使用してリアルタイム文字起こしを行う。
 * LINEAR16 (PCM 16bit) 16kHz モノラル音声に対応。
 */
@Component
@Profile("!test")
class GoogleStreamingSpeechTranscriber(
    private val speechClient: SpeechClient,
    cloudProperties: GoogleCloudProperties,
) : StreamingSpeechTranscriber {
    private val defaultLanguageCode: String = cloudProperties.speech.defaultLanguage
    private val logger = LoggerFactory.getLogger(GoogleStreamingSpeechTranscriber::class.java)

    override suspend fun startSession(config: StreamingTranscriptionConfig): StreamingTranscriptionSession {
        return GoogleStreamingTranscriptionSession(
            speechClient = speechClient,
            config = config.copy(
                languageCode = config.languageCode.ifBlank { defaultLanguageCode }
            ),
            logger = logger,
        )
    }
}

/**
 * Google Cloud Speech Streaming セッション
 */
class GoogleStreamingTranscriptionSession(
    private val speechClient: SpeechClient,
    private val config: StreamingTranscriptionConfig,
    private val logger: org.slf4j.Logger,
) : StreamingTranscriptionSession {

    private val closed = AtomicBoolean(false)

    @Volatile
    private var requestObserver: ApiStreamObserver<StreamingRecognizeRequest>? = null

    /** セッション準備完了を通知するためのDeferred */
    private val readyDeferred = kotlinx.coroutines.CompletableDeferred<Boolean>()

    override val results: Flow<TranscriptionResult> = callbackFlow {
        val recognitionConfig = RecognitionConfig.newBuilder()
            .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
            .setSampleRateHertz(config.sampleRateHertz)
            .setLanguageCode(config.languageCode)
            .setEnableAutomaticPunctuation(true)
            .build()

        val streamingConfig = StreamingRecognitionConfig.newBuilder()
            .setConfig(recognitionConfig)
            .setInterimResults(config.enableInterimResults)
            .build()

        // レスポンスを受信するオブザーバー
        val responseObserver = object : ApiStreamObserver<StreamingRecognizeResponse> {
            override fun onNext(response: StreamingRecognizeResponse) {
                logger.info("Received response from Google Speech API: resultsCount={}", response.resultsCount)
                if (closed.get()) {
                    logger.warn("Response received but session is closed")
                    return
                }

                for (result in response.resultsList) {
                    logger.debug("Result: alternativesCount={}, isFinal={}", result.alternativesCount, result.isFinal)
                    if (result.alternativesCount > 0) {
                        val alternative = result.alternativesList[0]
                        val transcriptionResult = TranscriptionResult(
                            text = if (config.languageCode.startsWith("ja")) {
                                alternative.transcript.replace(" ", "")
                            } else {
                                alternative.transcript
                            },
                            isFinal = result.isFinal,
                        )
                        logger.info(
                            "Sending transcription result: text='{}', isFinal={}",
                            alternative.transcript,
                            result.isFinal
                        )
                        trySend(transcriptionResult)

                        if (result.isFinal) {
                            logger.debug("Final result: {}", alternative.transcript)
                        }
                    }
                }
            }

            override fun onError(t: Throwable) {
                logger.error("Streaming transcription error (onError called)", t)
                readyDeferred.completeExceptionally(t)
                close(t)
            }

            override fun onCompleted() {
                logger.info("Streaming transcription completed (onCompleted called)")
                close()
            }
        }

        // リクエストを送信するオブザーバーを取得
        requestObserver = speechClient.streamingRecognizeCallable()
            .bidiStreamingCall(responseObserver)

        // 設定リクエストを送信
        val configRequest = StreamingRecognizeRequest.newBuilder()
            .setStreamingConfig(streamingConfig)
            .build()
        requestObserver?.onNext(configRequest)

        logger.info(
            "Started streaming transcription session: languageCode={}, sampleRate={}",
            config.languageCode, config.sampleRateHertz
        )

        // gRPCストリームが確立されたので準備完了を通知
        readyDeferred.complete(true)

        awaitClose {
            closeSession()
        }
    }

    override suspend fun awaitReady(timeout: kotlin.time.Duration): Boolean {
        return try {
            kotlinx.coroutines.withTimeout(timeout) {
                readyDeferred.await()
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            logger.warn("Timeout waiting for session to be ready: {}ms", timeout.inWholeMilliseconds)
            false
        } catch (e: Exception) {
            logger.error("Error waiting for session to be ready", e)
            false
        }
    }

    override suspend fun sendAudio(audioData: ByteArray) {
        if (closed.get()) {
            logger.warn("sendAudio called but session is closed")
            return
        }

        val observer = requestObserver
        if (observer == null) {
            logger.warn(
                "sendAudio called but requestObserver is null (Flow not yet collected). audioData size: {}",
                audioData.size
            )
            return
        }

        val audioRequest = StreamingRecognizeRequest.newBuilder()
            .setAudioContent(ByteString.copyFrom(audioData))
            .build()

        try {
            observer.onNext(audioRequest)
            logger.debug("Sent audio chunk: {} bytes", audioData.size)
        } catch (e: Exception) {
            if (!closed.get()) {
                logger.error("Error sending audio data", e)
            }
        }
    }

    override suspend fun close() {
        closeSession()
    }

    private fun closeSession() {
        if (closed.getAndSet(true)) {
            logger.debug("closeSession called but already closed")
            return
        }

        logger.info("Closing streaming transcription session (closeSession called)")
        val stackTrace = Thread.currentThread().stackTrace.take(10).joinToString("\n  ") { it.toString() }
        logger.info("closeSession stack trace:\n  {}", stackTrace)
        try {
            requestObserver?.onCompleted()
        } catch (e: Exception) {
            logger.warn("Error closing request observer", e)
        }
    }
}
