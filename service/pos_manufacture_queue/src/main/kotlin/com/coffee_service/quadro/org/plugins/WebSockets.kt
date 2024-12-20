package com.coffee_service.quadro.org.plugins

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration

fun Application.configureWebSockets(clients: MutableSet<DefaultWebSocketServerSession>) {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        webSocket("/updates") {
            clients += this
            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        outgoing.send(Frame.Text("Server received: ${frame.readText()}"))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                clients -= this
            }
        }
    }
}
