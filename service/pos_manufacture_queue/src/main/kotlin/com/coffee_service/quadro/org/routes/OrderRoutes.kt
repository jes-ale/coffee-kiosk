package com.coffee_service.quadro.org.routes

import com.coffee_service.quadro.org.model.Order
import com.coffee_service.quadro.org.objects.OrderCache.addLast
import com.coffee_service.quadro.org.objects.OrderCache.getNext
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

fun Route.order() {
    route("/order") {
        authenticate("auth-jwt") {
            get {
                // Retrieves the next order object
                val order = getNext()
                if (order != null) call.respond(order)
                else call.respond(HttpStatusCode.InternalServerError, TextPayload("Orders empty"))
            }
            post {
                // Appends the given order object at the bottom of the order queue
                val order = call.receive<Order>()
                if (addLast(order)) call.respond(HttpStatusCode.OK, TextPayload("PoS order stored"))
                else
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        TextPayload("PoS order not stored")
                    )
            }
        }
    }
}

@Serializable
data class TextPayload(val msg: String)
