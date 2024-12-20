package com.coffee_service.quadro.org.rpc

import com.coffee_service.quadro.org.model.*
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.json.*
import org.apache.xmlrpc.XmlRpcRequest
import org.apache.xmlrpc.client.AsyncCallback
import org.apache.xmlrpc.client.XmlRpcClient
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl

@ExperimentalCoroutinesApi
object RpcApi {
    private val client = XmlRpcClient()
    private val common_config = XmlRpcClientConfigImpl()
    private val models_config = XmlRpcClientConfigImpl()
    private var uid = 0
    private var db = ""
    private var api_key = ""
    private var host = ""

    fun version(
            target: (Any) -> Any, // Función anonima
            onError: (XmlRpcRequest, Throwable) -> Unit,
    ): Any? {
        return runCatching {
                    common_config.serverURL = URL("https://${this.host}/xmlrpc/2/common")

                    client.executeAsync(
                            common_config,
                            "version",
                            listOf<Any>(),
                            object : AsyncCallback {
                                override fun handleResult(pRequest: XmlRpcRequest, pResult: Any) {
                                    target(pResult)
                                }

                                override fun handleError(
                                        pRequest: XmlRpcRequest,
                                        pError: Throwable
                                ) {
                                    onError(pRequest, pError)
                                }
                            }
                    )
                }
                .getOrDefault(null)
    }

    fun login(username: String, apiKey: String, host: String, database: String): Int? {
        return runCatching {
                    this.host = host
                    common_config.serverURL = URL("https://${this.host}/xmlrpc/2/common")
                    this.uid =
                            client.execute(
                                    common_config,
                                    "authenticate",
                                    listOf(database, username, apiKey, listOf<Any>())
                            ) as
                                    Int
                    this.api_key = apiKey
                    this.db = database
                    models_config.serverURL = URL("https://${this.host}/xmlrpc/2/object")
                    return this.uid
                }
                .getOrDefault(null)
    }

    fun logout(): Boolean {
        this.uid = 0
        this.api_key = ""
        this.db = ""
        return true
    }

    private inline fun <reified T> kwQuery(
            pMethodName: String,
            model: String,
            kw: String,
            domain: Map<String, Any>,
            params: List<Any>,
    ): List<T> {

        require(pMethodName.isNotBlank()) { "pMethodName no puede estar vacío" }
        require(model.isNotBlank()) { "model no puede estar vacío" }
        require(kw.isNotBlank()) { "kw no puede estar vacío" }
        require(domain.isNotEmpty()) { "domain no puede estar vacío" }
        require(params.isNotEmpty()) { "params no puede estar vacío" }

        val sanitizedParams = params.filterNotNull()

        val result: Any? =
                try {
                    client.execute(
                            models_config,
                            pMethodName,
                            listOf(
                                    this.db,
                                    this.uid,
                                    this.api_key,
                                    model,
                                    kw,
                                    sanitizedParams,
                                    domain
                            )
                    )
                } catch (e: Exception) {
                    throw RuntimeException("Error al ejecutar la consulta: ${e.message}", e)
                }

        val resultArray: Array<*> =
                when (result) {
                    is Array<*> -> result
                    else ->
                            throw IllegalStateException(
                                    "El resultado de la consulta no es un Array"
                            )
                }

        return resultArray.mapNotNull { element ->
            try {
                Json.decodeFromJsonElement<T>(element.toJsonElement())
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun kwCall(
            pMethodName: String,
            model: String,
            kw: String,
            domain: Map<String, Any> = mapOf(),
            params: List<Any?>,
    ): Any {

        require(pMethodName.isNotBlank()) { "pMethodName no puede estar vacío" }
        require(model.isNotBlank()) { "model no puede estar vacío" }
        require(kw.isNotBlank()) { "kw no puede estar vacío" }
        require(params.isNotEmpty()) { "params no puede estar vacío" }

        val sanitizedParams = params.filterNotNull()

        return try {
            client.execute(
                    models_config,
                    pMethodName,
                    listOf(
                            this.db,
                            this.uid,
                            this.api_key,
                            model,
                            kw,
                            sanitizedParams, // Se envían solo parámetros no nulos
                            domain
                    )
            )
        } catch (e: Exception) {
            throw RuntimeException("Error al ejecutar la consulta: ${e.message}", e)
        }
    }

    fun confirmSingle(id: String): String {
        try {
            val res =
                    kwCall(
                            pMethodName = "execute_kw",
                            model = "mrp.production",
                            kw = "confirm_single",
                            params = listOf(mapOf("custom_uid" to id))
                    )
            return res as String
        } catch (e: Exception) {
            throw RuntimeException("Error al realizar la consulta a la API: ${e.message}", e)
        }
    }

    fun markAsDone(id: String) {
        try {
            version(
                    target = {
                        kwCall(
                                pMethodName = "execute_kw",
                                model = "mrp.production",
                                kw = "mark_as_done",
                                params = listOf(mapOf("custom_uid" to id))
                        )
                    },
                    onError = { _, _ -> }
            )
        } catch (e: Exception) {
            throw RuntimeException("Error al realizar la consulta a la API: ${e.message}", e)
        }
    }

    suspend fun queryProducts(): List<ProductPayload> {
        val domain =
                mutableMapOf<String, Any>().apply {
                    this["fields"] =
                            listOf(
                                    "id",
                                    "display_name",
                                    "categ_id",
                                    "pos_categ_id",
                                    "pos_production"
                            )
                    this["limit"] = 100
                }

        val resultDeferred = CompletableDeferred<List<Product>?>()

        try {
            version(
                    target = {
                        val products =
                                kwQuery<Product>(
                                        pMethodName = "execute_kw",
                                        model = "product.product",
                                        kw = "search_read",
                                        domain = domain.toMap(),
                                        params =
                                                listOf(
                                                        listOf(
                                                                listOf(
                                                                        "available_in_pos",
                                                                        "=",
                                                                        "True"
                                                                ),
                                                        )
                                                )
                                )
                        resultDeferred.complete(products)
                    },
                    onError = { _, _ ->
                        resultDeferred.complete(null) // Completar con null en caso de error
                    }
            )
        } catch (e: Exception) {
            throw RuntimeException("Error al realizar la consulta a la API: ${e.message}", e)
        }

        val payload = resultDeferred.await()

        if (payload.isNullOrEmpty())
                throw RuntimeException(
                        "Error al realizar la consulta a la API: No se pudo obtener el resultado. ${payload}"
                )

        return payload.map { prod ->
            // Validación de campos nulos
            val categId =
                    prod.categ_id?.get(1)
                            ?: throw IllegalArgumentException("Campo categ_id está vacío")
            val posCategId =
                    prod.pos_categ_id?.get(1)
                            ?: throw IllegalArgumentException("Campo pos_categ_id está vacío")

            // -- Manejo de excepciones en la deserialización -- //

            val categ: String =
                    try {
                        Json.decodeFromJsonElement(categId)
                    } catch (e: Exception) {
                        throw IllegalArgumentException(
                                "Error al decodificar categ_id: ${e.message}",
                                e
                        )
                    }

            val posCateg: String =
                    try {
                        Json.decodeFromJsonElement(posCategId)
                    } catch (e: Exception) {
                        throw IllegalArgumentException(
                                "Error al decodificar pos_categ_id: ${e.message}",
                                e
                        )
                    }

            ProductPayload(prod.id, prod.display_name, categ, posCateg)
        }
    }

    fun createProduction(map: Map<String, Any>): Any {
        val res =
                kwCall(
                        pMethodName = "execute_kw",
                        model = "mrp.production",
                        kw = "create_single",
                        params = listOf(map)
                ) as
                        String
        return res
    }

    private fun queryStockMove(ids: List<Int>): List<StockMove> {
        try {
            val domain = mutableMapOf<String, Any>()
            domain["fields"] = listOf("id", "product_id", "product_uom_qty")

            val stockMoves =
                    kwQuery<StockMove>(
                            pMethodName = "execute_kw",
                            model = "stock.move",
                            kw = "read",
                            domain = domain.toMap(),
                            params = listOf(ids)
                    )

            if (stockMoves.isEmpty()) throw RuntimeException("Stock Move: Resultado vacio.")

            return stockMoves
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun queryProduction(): List<ProductionPayload> {
        val domain = mutableMapOf<String, Any>()
        domain["fields"] =
                listOf(
                        "id",
                        "date_deadline",
                        "date_finished",
                        "display_name",
                        "origin",
                        "custom_uid",
                        "name",
                        "priority",
                        "product_qty",
                        "state",
                        "product_id",
                        "move_raw_ids",
                        "custom_uid",
                        "origin_unique_name",
                        "create_date"
                ) // TODO: generate field list based on serializable Model fields
        domain["limit"] = 100 // TODO: allow customize limit by user interface

        val resultDeferred = CompletableDeferred<List<Production>>()

        try {
            // Calcula la fecha y hora de hace 24 horas
            val now = LocalDateTime.now()
            val twentyFourHoursAgo = now.minusHours(24)

            // Convierte la fecha a formato compatible con Odoo (ISO 8601)
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val formattedDate = twentyFourHoursAgo.format(formatter)

            version(
                    target = {
                        val payload =
                                kwQuery<Production>(
                                        pMethodName = "execute_kw",
                                        model = "mrp.production",
                                        kw = "search_read",
                                        domain = domain.toMap(),
                                        params =
                                                listOf(
                                                        listOf(
                                                                listOf(
                                                                        "state",
                                                                        "in",
                                                                        listOf("progress", "draft")
                                                                )
                                                        )
                                                ),
                                )
                        resultDeferred.complete(payload)
                    },
                    onError = { _, _ -> resultDeferred.complete(emptyList()) }
            )
        } catch (e: Exception) {
            throw RuntimeException("Error al realizar la consulta a la API: ${e.message}", e)
        }

        val payload = resultDeferred.await()

        if (payload.isEmpty())
                throw RuntimeException(
                        "Error al realizar la consulta a la API: No se pudo obtener el resultado."
                )

        val body = mutableListOf<ProductionPayload>()
        for (production in payload) {
            val productId = Json.decodeFromJsonElement<Int>(production.product_id[0])
            val productDisplayName = Json.decodeFromJsonElement<String>(production.product_id[1])
            val rawStockMoveIds = Json.decodeFromJsonElement<List<Int>>(production.move_raw_ids)
            val rawStockMove = queryStockMove(rawStockMoveIds)
            val components = mutableListOf<ComponentPayload>()
            for (comp in rawStockMove) {
                components.add(
                        ComponentPayload(
                                Json.decodeFromJsonElement<Int>(comp.product_id[0]),
                                display_name =
                                        Json.decodeFromJsonElement<String>(comp.product_id[1]),
                                qty = comp.product_uom_qty
                        )
                )
            }
            body.add(
                    ProductionPayload(
                            id = production.id,
                            display_name = production.display_name,
                            origin = production.origin ?: "no-display",
                            priority = production.priority,
                            state = production.state,
                            product =
                                    ProductPaiload(
                                            id = productId,
                                            display_name = productDisplayName,
                                    ),
                            component = components,
                            pos_sync = false,
                            db_sync = true,
                            kitchen_sync = false,
                            custom_uid = production.custom_uid,
                            timestamp = production.create_date,
                            origin_unique_name = production.origin_unique_name,
                            production_delta = 0
                    )
            )
        }
        return body
    }

    /** https://github.com/Kotlin/kotlinx.serialization/issues/746#issuecomment-737000705 */
    private fun Any?.toJsonElement(): JsonElement {
        return when (this) {
            is Number -> JsonPrimitive(this)
            is Boolean -> JsonPrimitive(this)
            is String -> JsonPrimitive(this)
            is Array<*> -> this.toJsonArray()
            is List<*> -> this.toJsonArray()
            is Map<*, *> -> this.toJsonObject()
            is JsonElement -> this
            else -> JsonNull
        }
    }

    private fun Array<*>.toJsonArray(): JsonArray {
        val array = mutableListOf<JsonElement>()
        this.forEach { array.add(it.toJsonElement()) }
        return JsonArray(array)
    }

    private fun List<*>.toJsonArray(): JsonArray {
        val array = mutableListOf<JsonElement>()
        this.forEach { array.add(it.toJsonElement()) }
        return JsonArray(array)
    }

    private fun Map<*, *>.toJsonObject(): JsonObject {
        val map = mutableMapOf<String, JsonElement>()
        this.forEach {
            if (it.key is String) {
                map[it.key as String] = it.value.toJsonElement()
            }
        }
        return JsonObject(map)
    }
}
