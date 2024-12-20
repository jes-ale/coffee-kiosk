package com.coffee_service.quadro.org.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.httpsredirect.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.*

fun Application.configureHTTP() {
  routing { swaggerUI(path = "openapi") }
  /* install(HttpsRedirect) {
    // The port to redirect to. By default, 443, the default HTTPS port.
    sslPort = 443
    // 301 Moved Permanently, or 302 Found redirect.
    permanentRedirect = true
  } */
  install(CORS) {
    allowMethod(HttpMethod.Options)
    allowMethod(HttpMethod.Put)
    allowMethod(HttpMethod.Delete)
    allowMethod(HttpMethod.Patch)
    allowHeader(HttpHeaders.Authorization)
    allowHeader(HttpHeaders.ContentType)
    allowHeader(HttpHeaders.Accept)
    allowHost("coffee2.quadrosoluciones.mx", schemes = listOf("http", "https"))
    allowHost("www.coffee2.quadrosoluciones.mx", schemes = listOf("http", "https"))
    allowHost("coffee2-test.quadrosoluciones.mx", schemes = listOf("http", "https"))
    allowHost("grupohispanica-main-17065487.dev.odoo.com", schemes = listOf("http", "https"))
    allowHost("grupohispanica.odoo.com", schemes = listOf("http", "https"))
    allowHost("localhost:3000", schemes = listOf("http", "https"))
    allowHost("localhost", schemes = listOf("http", "https", "tauri"))
    allowHost("127.0.0.1", schemes = listOf("http", "https"))
    allowHost("192.168.2.70", schemes = listOf("http", "https"))
    allowHost("192.168.2.71", schemes = listOf("http", "https"))
    allowHost("192.168.2.72", schemes = listOf("http", "https"))
    allowHost("192.168.2.73", schemes = listOf("http", "https"))
    allowHost("192.168.2.74", schemes = listOf("http", "https"))
    allowHost("192.168.2.75", schemes = listOf("http", "https"))
    allowSameOrigin = true
  }
}
