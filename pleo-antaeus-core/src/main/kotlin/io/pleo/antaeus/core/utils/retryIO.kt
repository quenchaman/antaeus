package io.pleo.antaeus.core.utils

import kotlinx.coroutines.delay

// https://stackoverflow.com/questions/46872242/how-to-exponential-backoff-retry-on-kotlin-coroutines with modifications
internal suspend fun retryIO(
    times: Int = Int.MAX_VALUE,
    initialDelay: Long = 100,
    maxDelay: Long = 1000,
    factor: Double = 2.0,
    block: suspend () -> Boolean): Boolean
{
    var currentDelay = initialDelay
    repeat(times - 1) {
        val success: Boolean = block()
        println("Retry dage: " + !success)

        if (success) {
            return success
        }

        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }

    return block()
}