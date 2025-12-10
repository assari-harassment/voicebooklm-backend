package com.assari.voicebooklm.application.support

import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * 処理の経過時間を計測するポート。
 */
interface ExecutionTimer {
    suspend fun <T> measure(block: suspend () -> T): TimedResult<T>
}

/**
 * 計測結果。
 */
data class TimedResult<T>(
    val value: T,
    val duration: Duration,
)

/**
 * モノトニック時計を利用した標準計測器。
 */
class MonotonicExecutionTimer(
    private val timeSource: TimeSource = TimeSource.Monotonic,
) : ExecutionTimer {
    override suspend fun <T> measure(block: suspend () -> T): TimedResult<T> {
        val mark = timeSource.markNow()
        val value = block()
        return TimedResult(value, mark.elapsedNow())
    }
}
