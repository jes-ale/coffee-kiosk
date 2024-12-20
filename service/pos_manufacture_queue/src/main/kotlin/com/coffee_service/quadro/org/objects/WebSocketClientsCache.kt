package com.coffee_service.quadro.org.objects

import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.util.*

object WebSocketClients {
    val clients: MutableSet<DefaultWebSocketServerSession> =
            Collections.synchronizedSet(LinkedHashSet<DefaultWebSocketServerSession>())

    suspend fun notifyClients(
            clients: Set<DefaultWebSocketServerSession>,
            type: String,
            data: String? = null,
    ) {
        val message = "refresh"

        clients.forEach { session ->
            try {
                session.send(Frame.Text(message))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
