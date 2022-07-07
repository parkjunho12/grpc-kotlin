package io.grpc.examples.helloworld.util

import chatserver.Chat
import io.grpc.*
import io.grpc.examples.helloworld.Ready
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object CustomGRPC {

    sealed class Request<RequestT> {
        /**
         * Send the request(s) to the ClientCall, with `readiness` indicating calls to `onReady` from
         * the listener.  Returns when sending the requests is done, either because all the requests
         * were sent (in which case `null` is returned) or because the requests channel was closed
         * with an exception (in which case the exception is returned).
         */
        abstract suspend fun sendTo(
            clientCall: ClientCall<RequestT, *>,
            readiness: Ready
        )

        class Unary<RequestT>(private val request: RequestT) : Request<RequestT>() {
            override suspend fun sendTo(
                clientCall: ClientCall<RequestT, *>,
                readiness: Ready
            ) {
                readiness.suspendUntilReady()
                clientCall.sendMessage(request)
            }
        }

        class Flowing<RequestT>(private val requestFlow: Flow<RequestT>) : Request<RequestT>() {
            override suspend fun sendTo(
                clientCall: ClientCall<RequestT, *>,
                readiness: Ready
            ) {
                readiness.suspendUntilReady()
                requestFlow.collect { request ->
                    clientCall.sendMessage(request)
                    readiness.suspendUntilReady()
                }
            }
        }

    }

    fun sendTo(request: Flow<Chat.FromClient>, method: MethodDescriptor<Chat.FromClient, Chat.FromServer>, clientCall: ClientCall<Chat.FromClient, Chat.FromServer>, readiness: Ready, responses: Channel<Chat.FromServer>) = flow {
        coroutineScope {
            val sender =
                CoroutineScope(Dispatchers.IO).launch(CoroutineName("SendMessage worker for ${method.fullMethodName}")) {
                    try {
                        Request.Flowing(request).sendTo(clientCall, readiness)
//                        clientCall.halfClose()
                    } catch (ex: Exception) {
                        clientCall.cancel(
                            "Collection of requests completed exceptionally",
                            ex
                        )
                        throw ex // propagate failure upward
                    }
                }
            try {
                clientCall.request(1)
                for (response in responses) {
                    emit(response)
                    clientCall.request(1)
                }
            } catch (e: Exception) {
                withContext(NonCancellable) {
                    sender.cancel("Collection of responses completed exceptionally", e)
                    sender.join()
                    // we want sender to be done cancelling before we cancel clientCall, or it might try
                    // sending to a dead call, which results in ugly exception messages
                    clientCall.cancel("Collection of responses completed exceptionally", e)
                }
                throw e
            }
            if (!sender.isCompleted) {
                sender.cancel("Collection of responses completed before collection of requests")
            }
        }
    }

    fun Metadata.copy(): Metadata {
        val result = Metadata()
        result.merge(this)
        return result
    }
}