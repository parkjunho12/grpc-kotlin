package io.grpc.examples.helloworld

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.telephony.TelephonyManager
import android.widget.Button
import android.widget.EditText
import androidx.core.app.ActivityCompat
import androidx.core.view.size
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import chatserver.Chat
import chatserver.ServicesGrpc
import chatserver.fromClient
import io.grpc.*
import io.grpc.examples.helloworld.util.CustomGRPC.copy
import io.grpc.examples.helloworld.adapter.MainAdapter
import io.grpc.examples.helloworld.dto.ChatItem
import io.grpc.examples.helloworld.util.CustomGRPC
import io.grpc.examples.helloworld.util.PhoneUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.flow.flow

class MainActivity : AppCompatActivity() {
    private val uri by lazy { Uri.parse("http://49.247.192.42:5000/") }
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
    private lateinit var linearLayoutManager: LinearLayoutManager
    val responses = Channel<Chat.FromServer>(1)
    val readiness = Ready { clientCall.isReady }
    private var curList: ArrayList<ChatItem> = arrayListOf()
    private lateinit var mainAdapter: MainAdapter
    private var roomStr = ""
    private lateinit var recyclerView: RecyclerView
    private lateinit var sendBtn: Button
    private lateinit var etText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sendBtn = findViewById<Button>(R.id.send_btn)
        etText = findViewById<EditText>(R.id.et_main)
        recyclerView = findViewById<RecyclerView>(R.id.main_recyclerview)
        linearLayoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = linearLayoutManager
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED) {
            runClientResponse()
        } else {
            requestPermissions(arrayOf(Manifest.permission.READ_PHONE_NUMBERS), 101)
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode) {
            101 -> {
                runClientResponse()
            }
            else -> {

            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun runClientResponse() {
        val method = ServicesGrpc.getChatServiceMethod()
        clientCall =
            channel.newCall(method, CallOptions.DEFAULT)
        val header = Metadata()
        val pnumber: Metadata.Key<String> =
            Metadata.Key.of("pnumber", Metadata.ASCII_STRING_MARSHALLER)
        val room: Metadata.Key<String> =
            Metadata.Key.of("room", Metadata.ASCII_STRING_MARSHALLER)
        header.put(pnumber, PhoneUtil.getSimPNumber(this))
        header.put(room, roomStr)
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
            header.copy()
        )
        mainAdapter = MainAdapter(curList)
        recyclerView.adapter = mainAdapter

        sendBtn.setOnClickListener {
            val etString = etText.text.toString()
            lifecycleScope.launchWhenCreated {
                sendTo(etString = etString, method).collect {
                    curList.add(ChatItem(it.body, it.pnumber, it.pnumber == PhoneUtil.getSimPNumber(this@MainActivity)))
                    mainAdapter.notifyItemSet(curList.lastIndex)
                    recyclerView.adapter = mainAdapter
                    recyclerView.scrollToPosition(curList.lastIndex)
                    etText.setText("")
                }
            }
        }
    }

    private fun sendTo(etString: String, method: MethodDescriptor<Chat.FromClient, Chat.FromServer>) = CustomGRPC.sendTo(
        flow {
        emit(fromClient {
            this.pnumber = PhoneUtil.getSimPNumber(this@MainActivity)
            this.body = etString
        })
    }, method, clientCall, readiness, responses)


    override fun onDestroy() {
        super.onDestroy()
        clientCall.halfClose()
        channel.shutdownNow()
    }
}