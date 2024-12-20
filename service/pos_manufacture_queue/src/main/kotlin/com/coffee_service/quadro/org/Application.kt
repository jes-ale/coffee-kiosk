package com.coffee_service.quadro.org

import com.coffee_service.quadro.org.objects.WebSocketClients.clients
import com.coffee_service.quadro.org.plugins.*
import io.ktor.server.application.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
        configureSerialization()
        configureSecurity()
        configureRouting()
        configureHTTP()
        configureSSL()
        configureWebSockets(clients)
        schedulePeriodicSync(clients)
}
