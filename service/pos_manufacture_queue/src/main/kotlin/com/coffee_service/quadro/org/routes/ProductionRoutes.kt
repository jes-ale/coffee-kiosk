package com.coffee_service.quadro.org.routes

import com.coffee_service.quadro.org.model.ProductionOrderBody
import com.coffee_service.quadro.org.objects.ProductionCache.createNewProductionItem
import com.coffee_service.quadro.org.objects.ProductionCache.getCache
import com.coffee_service.quadro.org.objects.ProductionCache.getNext
import com.coffee_service.quadro.org.objects.ProductionCache.getProductionItemFromCache
import com.coffee_service.quadro.org.objects.ProductionCache.getQueue
import com.coffee_service.quadro.org.objects.ProductionCache.productionToSync
import com.coffee_service.quadro.org.objects.ProductionCache.setNext
import com.coffee_service.quadro.org.objects.ProductionCache.syncDb
import com.coffee_service.quadro.org.objects.ProductionCache.updateCache
import com.coffee_service.quadro.org.rpc.RpcApi.markAsDone
import com.coffee_service.quadro.org.rpc.RpcApi.queryProducts
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.Serializable

@OptIn(ExperimentalCoroutinesApi::class)
fun Route.production() {
    route("/MarkSingleScrap") {
        authenticate("auth-jwt") { post { call.respond(HttpStatusCode.OK, "OKEI") } }
    }
    route("/UnlinkAll") {
        authenticate("auth-jwt") { post { call.respond(HttpStatusCode.OK, "OKEI") } }
    }
    route("/UnlinkSingle") {
        authenticate("auth-jwt") { post { call.respond(HttpStatusCode.OK, "OKEI") } }
    }
    route("/MarkCurrentOrderScrap") {
        authenticate("auth-jwt") { post { call.respond(HttpStatusCode.OK, "OKEI") } }
    }
    route("/syncDb") {
        authenticate("auth-jwt") {
            get {
                val res = runCatching { syncDb() }.getOrDefault(false)
                call.respond(HttpStatusCode.OK, "$res")
            }
        }
    }
    route("/popProductionQueue") {
        authenticate("auth-jwt") {
            get {
                // Pops the queue and retrives the item from cache, does not clear the item from
                // cache
                // Retrieves the next production item from cache
                val body = getNext()
                if (body == null) call.respond(HttpStatusCode.OK, listOf<String>())
                else call.respond(HttpStatusCode.OK, body)
            }
        }
    }
    route("/setDoneProduction") {
        post {
            try {
                // Marks the given production item as done
                val id = call.receive<IdPayload>()
                markAsDone(id.id)
                call.respond(HttpStatusCode.OK, id.id)
            } catch (e: Exception) {
                call.respond(
                        HttpStatusCode.InternalServerError,
                        UidPayload("Exception occurred: ${e.message}")
                )
            }
        }
    }
    route("/confirmProduction") {
        post {
            try {
                val id = call.receive<IdPayload>()
                val productionItem = getProductionItemFromCache(id.id)
                if (productionItem != null) {
                    // Update the item in the cache to mark it as confirmed
                    productionItem.state = "progress"
                    updateCache(listOf(productionItem))
                    call.respond(HttpStatusCode.OK, productionItem)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Production item not found in cache")
                }
            } catch (e: Exception) {
                call.respond(
                        HttpStatusCode.InternalServerError,
                        UidPayload("Exception occurred: ${e.message}")
                )
            }
        }
    }
    route("/setNextProduction") {
        authenticate("auth-jwt") {
            post {
                // IMPORTANT NOTE ABOUT THIS ENDPOINT:
                // Appends the given ** order name ** at the bottom of the production queue.
                // It's used from all PoS when finalizing a sale operation (onClickSend or
                // onClickPay).
                // Appends the name of the order to the queue and appends it's mrp.production items
                // to the cache
                val body = call.receive<ProductionOrderBody>()
                productionToSync.add(body) // to send to odoo
                val item = createNewProductionItem(body) // to send to web app front-end
                setNext(origin = body.origin, item = item)
                call.respond(HttpStatusCode.OK, item)
            }
        }
    }
    route("/getProductionQueue") {
        authenticate("auth-jwt") {
            get {
                // Retrieves the current state of the cached production items
                val cache = getQueue()
                call.respond(HttpStatusCode.OK, cache)
            }
        }
    }
    route("/getProductionCache") {
        authenticate("auth-jwt") {
            get {
                // Retrieves the current state of the cached production items
                val cache = getCache()
                call.respond(HttpStatusCode.OK, cache)
            }
        }
    }
    route("/getProducts") {
        authenticate("auth-jwt") {
            get {
                try {
                    // Used for locally caching the products on the frontend.
                    val prods = queryProducts()
                    call.respond(HttpStatusCode.OK, prods)
                } catch (e: Exception) {
                    call.respond(
                            HttpStatusCode.InternalServerError,
                            UidPayload("Exception occurred: ${e.toString()}")
                    )
                }
            }
        }
    }
}

@Serializable data class IdPayload(val id: String)

@Serializable data class UidPayload(val uid: String, val cstatuscode: String? = null)
