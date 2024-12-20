package com.coffee_service.quadro.org.objects

import com.coffee_service.quadro.org.model.ProductPaiload
import com.coffee_service.quadro.org.model.ProductionOrderBody
import com.coffee_service.quadro.org.model.ProductionPayload
import com.coffee_service.quadro.org.objects.WebSocketClients.clients
import com.coffee_service.quadro.org.objects.WebSocketClients.notifyClients
import com.coffee_service.quadro.org.rpc.RpcApi.createProduction
import com.coffee_service.quadro.org.rpc.RpcApi.queryProduction
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * The ProductionCache object manages the caching and queuing of production items (mrp.production).
 * It follows the FIFO (First In, First Out) principle to handle production items efficiently.
 *
 * The object provides functionality for:
 * - Caching production items by their origin (order name).
 * - Maintaining a production queue to track the order of processing.
 * - Synchronizing the production data with an external database (like Odoo).
 * - Detecting and handling changes between the cached production data and the data stored in the
 * database.
 *
 * Components:
 * - `productionCache`: A mutable map that stores lists of production items indexed by their origin
 * (order name).
 * - `productionQueue`: A mutable list that represents the queue of order names to be processed.
 * - `productionToSync`: A mutable list that stores production items awaiting synchronization with
 * the database.
 *
 * Key Methods:
 * - `syncDb()`: Synchronizes the cache with the database and returns whether the cache has been
 * updated.
 * - `setNext()`: Adds a new production item to the cache and appends the corresponding order name
 * to the queue.
 * - `getNext()`: Pops the next production item from the queue and retrieves it from the cache.
 * - `cleanDone()`: Updates the cache by filtering out completed production items.
 * - `hasCacheChanged()`: Compares two caches to determine if any changes have occurred.
 * - `getQueue()`: Retrieves the current production queue.
 * - `getCache()`: Retrieves the current state of the production cache.
 * - `createNewProductionItem()`: Creates a new production item from a production order body.
 *
 * Usage:
 * - This object is intended to be used in scenarios where production items need to be cached,
 * queued, and synchronized with a remote database. It supports FIFO operations and ensures that the
 * cached data remains consistent with the data in the database.
 */
object ProductionCache {
    // Cached production items by order name
    private val productionCache = mutableMapOf<String, List<ProductionPayload>>()

    // Queue of order names
    private val productionQueue = mutableListOf<String>()

    // Received queue of mrp.production waiting for sync db to be called
    var productionToSync = mutableListOf<ProductionOrderBody>()

    /**
     * Synchronizes the cached production items with the external database (e.g., Odoo).
     *
     * This method checks for existing production records in the database, compares the cached items
     * to detect changes, and creates new records in the database for any production items that are
     * not already present. The cache is updated accordingly after each sync.
     *
     * @return Boolean indicating whether the cache was updated.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun syncDb(): Any? {
        // 1. Helpers
        var cacheUpdated = false
        var res: Any? = null
        val toDeleteCustomUIDs: MutableList<String> = mutableListOf()
        val oldCache = getCache() // Helper to hold the old cache

        // 2. Query DB
        var existingProductions =
                runCatching { queryProduction() }.getOrDefault(emptyList()).map {
                    it.apply { it.db_sync = true }
                }

        // 3. Update the cache given the queried production items
        updateCache(existingProductions)
        var newCache = getCache() // Just a holder for the new cache
        // 5. Check for remote updates (update remote with new or finished items) and create / updates the items
        productionToSync.forEach { item ->
            val toSyncCustomUidList =
                    newCache[item.origin]?.filter { !it.db_sync }?.map { it.custom_uid }
            if (toSyncCustomUidList != null && toSyncCustomUidList.contains(item.custom_uid)) {
                res =
                        createProduction(
                                map =
                                        mapOf(
                                                "pos_reference" to item.origin,
                                                "product_id" to "${item.product_id}",
                                                "state" to item.state,
                                                "product_tmpl_id" to "${item.product_tmpl_id}",
                                                "product_uom_id" to "${item.product_uom_id}",
                                                "product_qty" to "${item.product_qty}",
                                                "bom_id" to "${item.bom_id}",
                                                "custom_uid" to item.custom_uid,
                                                "components" to
                                                        item.extra_components.associate {
                                                            it.id to it.qty
                                                        }
                                        )
                        )
                toDeleteCustomUIDs.apply { add(item.custom_uid) }
            }
        }

        existingProductions =
                runCatching { queryProduction() }.getOrDefault(emptyList()).map {
                    it.apply { it.db_sync = true }
                }

        // 4. Update the cache given the computed production items
        updateCache(existingProductions)

        newCache = getCache()

        val toDeleteCustomUIDsSet = toDeleteCustomUIDs.toSet()
        productionToSync.removeAll { item -> item.custom_uid in toDeleteCustomUIDsSet }

        if (hasCacheChanged(oldCache, newCache)) {
            return true
        }

        return res
    }

    /**
     * Compares two caches and returns whether they are different.
     *
     * @param oldCache The original cache before updates.
     * @param newCache The updated cache after sync.
     * @return Boolean indicating if the cache has changed.
     */
    private fun hasCacheChanged(
            oldCache: Map<String, List<ProductionPayload>>,
            newCache: Map<String, List<ProductionPayload>>,
    ): Boolean {
        if (oldCache.size != newCache.size) return true

        for ((origin, oldItems) in oldCache) {
            val newItems = newCache[origin] ?: return true

            if (oldItems.size != newItems.size) return true

            if (oldItems.any { item -> !newItems.contains(item) }) return true
        }

        return false
    }

    /**
     * Updates the production cache with the current list of production items from the database,
     * ensuring that only modified or new items are updated for each 'origin'.
     *
     * - If an item with the same 'custom_uid' exists within the same 'origin', it will be updated.
     * - Otherwise, the new item will be added to the corresponding list of 'origin'.
     * - Completed production items will be filtered out.
     *
     * @param production The list of current production items to update the cache.
     */
    suspend fun updateCache(production: List<ProductionPayload>) {
        production.forEach { item ->
            val currentItems = productionCache[item.origin]?.toMutableList() ?: mutableListOf()
            val existingItemIndex = currentItems.indexOfFirst { it.custom_uid == item.custom_uid }
            if (existingItemIndex != -1) {
                // Some hacks :p these values are not in DB but only in-memory
                // We try to hold'em between updates of the same entry
                item.db_sync = currentItems[existingItemIndex].db_sync
                item.pos_sync = currentItems[existingItemIndex].pos_sync
                item.kitchen_sync = currentItems[existingItemIndex].kitchen_sync
                currentItems[existingItemIndex] = item
            } else {
                currentItems.add(item)
            }
            productionCache[item.origin] = currentItems
        }
        notifyClients(clients, "refresh")
    }

    /**
     * Appends the given order name (origin) to the end of the production queue (productionQueue)
     * and adds the new item (item) to the beginning of the list of production items
     * (mrp.production) in the cache associated with the origin (productionCache). This implements
     * the "First In" part of FIFO.
     *
     * - If there is no existing list for the 'origin' in the cache, a new list is created with
     * 'item'.
     * - If a list already exists, 'item' is added to the beginning of the current list.
     * - Then, 'origin' is appended to the end of the production queue (productionQueue) if it is
     * not already present.
     *
     * @param origin The production order name or identifier.
     * @param item The 'ProductionPayload' object representing a new production item to add.
     * @return The updated list of the production queue (productionQueue) containing the 'origin'.
     */
    suspend fun setNext(origin: String, item: ProductionPayload): List<String> {
        item.pos_sync = true

        // Verifica si ya existe un item con el mismo custom_uid para el origin
        val existingItems = productionCache[origin] ?: emptyList()
        if (existingItems.any { it.custom_uid == item.custom_uid }) {
            // Si ya existe un item con el mismo custom_uid, no lo agrega
            return productionQueue
        }

        // Si no existe, lo agrega al principio de la lista
        productionCache[origin] = mutableListOf(item).apply { addAll(existingItems) }

        // Añadir el origin al final de la producción, si no está ya presente
        if (!productionQueue.contains(origin)) {
            productionQueue.add(origin)
        }

        // Notificar a los clientes sobre la actualización
        notifyClients(clients, "refresh")

        return productionQueue
    }

    fun getProductionItemFromCache(customUid: String): ProductionPayload? {
        return productionCache.values.flatten().find { it.custom_uid == customUid }
    }

    /**
     * Pops the next item from the production queue and retrieves the corresponding production
     * object from the cache. This is the "First Out" part of FIFO.
     *
     * @return The list of ProductionPayload for the next order in the queue, or null if the queue
     * is empty.
     */
    fun getNext(): List<ProductionPayload>? =
            runCatching { productionCache[productionQueue.removeFirstOrNull()] }.getOrDefault(null)

    /**
     * Retrieves the current production queue.
     *
     * @return A list of order names in the queue.
     */
    fun getQueue(): List<String> {
        return productionQueue
    }

    /**
     * Retrieves the current state of the production cache.
     *
     * @return A map of order names to lists of ProductionPayload objects.
     */
    fun getCache(): Map<String, List<ProductionPayload>> {
        return productionCache
    }

    /**
     * Creates a new ProductionPayload item from a ProductionOrderBody.
     *
     * @param body The ProductionOrderBody containing the necessary details to create a
     * ProductionPayload.
     * @return The newly created ProductionPayload.
     */
    fun createNewProductionItem(body: ProductionOrderBody): ProductionPayload {
        return ProductionPayload(
                timestamp = "0",
                db_sync = false,
                pos_sync = false,
                custom_uid = body.custom_uid,
                origin_unique_name = body.origin_unique_name,
                production_delta = body.production_delta,
                kitchen_sync = false,
                product = ProductPaiload(id = body.product_id, display_name = body.display_name),
                id = 0,
                component = body.extra_components,
                display_name = body.display_name,
                state = body.state,
                origin = body.origin,
                priority = "high"
        )
    }
}
