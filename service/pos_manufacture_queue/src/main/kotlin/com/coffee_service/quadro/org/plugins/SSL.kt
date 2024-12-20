package com.coffee_service.quadro.org.plugins

import io.ktor.server.application.*
import io.ktor.network.tls.certificates.*
import java.io.*

fun Application.configureSSL() {
    val keyStoreFile = File("build/keystore.jks")
    val keyStore = buildKeyStore {
        certificate("sampleAlias") {
            password = "foobar"
            domains = listOf("127.0.0.1", "0.0.0.0", "localhost")
        }
    }
    keyStore.saveToFile(keyStoreFile, "foobar")
}

