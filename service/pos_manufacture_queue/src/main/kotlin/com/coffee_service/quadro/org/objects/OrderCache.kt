package com.coffee_service.quadro.org.objects

import com.coffee_service.quadro.org.model.Order

// NOTE:
// Only 1 data structure for holding orders, once popped there is no way back
// It might be wise to create a fallback structure as with the production orders to hold the orders
// in cache for some time and only clean up the ones that are done with...
// But as for now, we are going with a simple queue for the orders
// There's only a few use cases where complex data structures might be needed and them all involve
// human error or jumping the queue IRL (in real life)
object OrderCache {
        // Order objects queue
        private val orderQueue = mutableListOf<Order>()

        fun getNext(): Order? {
                // Pops the top order object from the queue
                return orderQueue.removeFirstOrNull()
        }

        fun addLast(order: Order): Boolean {
                // Appends the given origin order name to be at the bottom of the orders queue
                return orderQueue.add(order)
        }
}
