package io.grpc.examples.helloworld

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onFailure

class Ready(
    private val isReallyReady: () -> Boolean
) {
    // A CONFLATED channel never suspends to send, and two notifications of readiness are equivalent
    // to one
    private val channel = Channel<Unit>(Channel.CONFLATED)

    fun onReady() {
        channel.trySend(Unit).onFailure { e ->
            throw e ?: AssertionError(
                "Should be impossible; a CONFLATED channel should never return false on offer"
            )
        }
    }

    suspend fun suspendUntilReady() {
        while (!isReallyReady()) {
            channel.receive()
        }
    }
}