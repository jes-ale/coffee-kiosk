package com.coffee_service.quadro.org.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray

@Serializable
data class ProductionOrderBody(
        val display_name: String,
        val origin: String,
        val origin_unique_name: String,
        val custom_uid: String,
        val product_id: Int,
        val production_delta: Int,
        val product_qty: Int,
        val state: String,
        val product_tmpl_id: Int,
        val product_uom_id: Int,
        val bom_id: Int,
        val extra_components: List<ComponentPayload>,
)

@Serializable
data class ProductionPayload(
        val id: Int,
        val display_name: String,
        val origin: String,
        val origin_unique_name: String,
        val production_delta: Int,
        val priority: String,
        var state: String,
        val product: ProductPaiload,
        val component: List<ComponentPayload>,
        var db_sync: Boolean,
        var pos_sync: Boolean,
        var kitchen_sync: Boolean,
        val custom_uid: String,
        val timestamp: String
)

@Serializable data class ProductPaiload(val id: Int, val display_name: String)

@Serializable data class ComponentPayload(val id: Int, val display_name: String, val qty: Double)

@Serializable
data class Production(
        val id: Int,
        val date_deadline: Boolean,
        val date_finished: Boolean,
        val display_name: String,
        val origin_unique_name: String,
        val custom_uid: String,
        val create_date: String,
        val origin: String?,
        val name: String,
        val priority: String,
        val product_qty: Double,
        val state: String,
        val product_id: JsonArray,
        val move_raw_ids: JsonArray
)

@Serializable
data class StockMove(val id: Int, val product_id: JsonArray, val product_uom_qty: Double)
