package com.coffee_service.quadro.org.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.healthCheck() {
  route("/version") { get { call.respond(HttpStatusCode.OK, "Hello") } }
}
