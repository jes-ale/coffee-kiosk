package com.coffee_service.quadro.org.plugins

import com.coffee_service.quadro.org.objects.ProductionCache.syncDb
import com.coffee_service.quadro.org.objects.WebSocketClients.notifyClients
import io.ktor.server.application.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.*

// Define the coroutine that runs the periodic task
fun Application.schedulePeriodicSync(clients: Set<DefaultWebSocketServerSession>) {
    launch {
        while (true) {
            delay(180_000) // Delay of [3] minutes

            // Call sync_db and check if cache was updated
            val wasUpdated = syncDb()

            // If there was an update, notify clients via WebSocket
            if (wasUpdated !== null) {
                notifyClients(clients, "refresh")
            }
        }
    }
}
