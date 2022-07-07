package io.grpc.examples.helloworld

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import chatserver.Chat
import chatserver.ServicesGrpc
import chatserver.ServicesGrpcKt
import chatserver.fromClient
import io.grpc.*
import io.grpc.examples.helloworld.util.CustomGRPC
import io.grpc.examples.helloworld.util.CustomGRPC.copy
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.Closeable

class MainActivity2 : AppCompatActivity() {

    private val uri by lazy { Uri.parse("http://192.168.15.134:5000/") }
    private val greeterService by lazy { GreeterRCP(uri) }
    private val channel = let {
        println("Connecting to ${uri.host}:${uri.port}")

        val builder = ManagedChannelBuilder.forAddress(uri.host, uri.port)
        if (uri.scheme == "https") {
            builder.useTransportSecurity()
        } else {
            builder.usePlaintext()
        }
        builder.keepAliveWithoutCalls(true)

        builder.executor(Dispatchers.IO.asExecutor()).build()
    }
    private lateinit var clientCall: ClientCall<Chat.FromClient, Chat.FromServer>

    val responses = Channel<Chat.FromServer>(1)
    val readiness = Ready { clientCall.isReady }
    private val responseState = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Surface(color = MaterialTheme.colors.background) {
                Greeter(greeterService)
            }
        }

        val request = flow {
            emit(fromClient {
                this.pnumber = responseState.value
                this.body = responseState.value
            })
        }
        val requestChat = CustomGRPC.Request.Flowing(request)
        val method = ServicesGrpc.getChatServiceMethod()
        clientCall =
            channel.newCall(method, CallOptions.DEFAULT)
        clientCall.start(
            object : ClientCall.Listener<Chat.FromServer>() {
                override fun onMessage(message: Chat.FromServer) {
                    responses.trySend(message).onFailure { e ->
                        throw e ?: AssertionError("onMessage should never be called until responses is ready")
                    }
                }

                override fun onClose(status: Status, trailersMetadata: Metadata) {
                    responses.close(
                        cause = if (status.isOk) null else status.asException(trailersMetadata)
                    )
                }

                override fun onReady() {
                    readiness.onReady()
                }
            },
            Metadata().copy()
        )


    }


    override fun onDestroy() {
        super.onDestroy()
        greeterService.close()
    }
}

private suspend fun sendData(channel: io.grpc.Channel, request: Flow<Chat.FromClient>) {


}





class GreeterRCP(uri: Uri) : Closeable {
    val responseState = mutableStateOf("")

    private val channel = let {
        println("Connecting to ${uri.host}:${uri.port}")

        val builder = ManagedChannelBuilder.forAddress(uri.host, uri.port)
        if (uri.scheme == "https") {
            builder.useTransportSecurity()
        } else {
            builder.usePlaintext()
        }
        builder.keepAliveWithoutCalls(true)

        builder.executor(Dispatchers.IO.asExecutor()).build()
    }

    private val greeter = ServicesGrpcKt.ServicesCoroutineStub(channel)

    suspend fun sayHello(name: String) {
        try {

                val request = flow {
                    emit(fromClient {
                        this.pnumber = name
                        this.body = name
                    })
                }
            sendData(channel, request)

        } catch (e: Exception) {
            responseState.value = e.message ?: "Unknown Error"
            e.printStackTrace()
        }
    }

    override fun close() {
        channel.shutdownNow()
    }

    private suspend fun sendData(request: Flow<Chat.FromClient>) {
        greeter.chatService(request).collect {
            responseState.value = it.body
        }
    }
}

@Composable
fun Greeter(greeterRCP: GreeterRCP) {

    val scope = rememberCoroutineScope()

    val nameState = remember { mutableStateOf(TextFieldValue()) }

    Column(Modifier.fillMaxWidth().fillMaxHeight(), Arrangement.Top, Alignment.CenterHorizontally) {
        Text(stringResource(R.string.name_hint), modifier = Modifier.padding(top = 10.dp))
        OutlinedTextField(nameState.value, { nameState.value = it })

        Button({ scope.launch {
            greeterRCP.sayHello(nameState.value.text)

        } }, Modifier.padding(10.dp)) {
        Text(stringResource(R.string.send_request))
    }

        if (greeterRCP.responseState.value.isNotEmpty()) {
            Text(stringResource(R.string.server_response), modifier = Modifier.padding(top = 10.dp))
            Text(greeterRCP.responseState.value)
        }
    }
}
