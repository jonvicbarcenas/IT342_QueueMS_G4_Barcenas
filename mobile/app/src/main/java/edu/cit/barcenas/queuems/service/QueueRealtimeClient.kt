package edu.cit.barcenas.queuems.service

import edu.cit.barcenas.queuems.api.RetrofitClient
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class QueueRealtimeClient(
    private val userId: String,
    private val onQueueMessage: () -> Unit
) {
    private val client = OkHttpClient()
    private var socket: WebSocket? = null
    private var subscribed = false

    fun connect() {
        if (socket != null) return
        val wsUrl = RetrofitClient.BASE_URL
            .replace("https://", "wss://")
            .replace("http://", "ws://")
            .trimEnd('/') + "/ws-native"

        socket = client.newWebSocket(
            Request.Builder().url(wsUrl).build(),
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    sendFrame("CONNECT\naccept-version:1.1,1.0\nheart-beat:0,0\n\n\u0000")
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    if (text.contains("CONNECTED") && !subscribed) {
                        subscribed = true
                        sendFrame("SUBSCRIBE\nid:user-queue\ndestination:/topic/user/$userId\n\n\u0000")
                        return
                    }

                    if (text.contains("MESSAGE") && text.contains("/topic/user/$userId")) {
                        onQueueMessage()
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    socket = null
                    subscribed = false
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    socket = null
                    subscribed = false
                }
            }
        )
    }

    fun disconnect() {
        socket?.close(1000, "Activity closed")
        socket = null
        subscribed = false
    }

    private fun sendFrame(frame: String) {
        socket?.send(frame)
    }
}
