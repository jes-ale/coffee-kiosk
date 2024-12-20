package com.coffee_service.quadro.org.model

import kotlinx.serialization.Serializable

@Serializable data class Order(val name: String, val uid: String, val unique_name: String, val orderlines: List<OrderLine>)

@Serializable
data class OrderLine(
                val custom_uid: String?,
                val product_id: Int,
                val options: ProductOptions,
                val extra_components: List<Components>
)

@Serializable data class Components(val id: Int, val qty: Int, val display_name: String)

@Serializable
data class ProductOptions(
                val draftPackLotLines: Nothing?
                = null,
                val quantity: Int?,
                val price: Float?,
                val description: String?
)
