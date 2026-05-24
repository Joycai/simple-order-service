package com.joycai.orderservice.service

import com.joycai.orderservice.model.*
import com.joycai.orderservice.repository.*
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class OrderService(
    private val itemRepository: ItemRepository,
    private val cartItemRepository: CartItemRepository,
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val shipmentRepository: ShipmentRepository,
    private val shipmentItemRepository: ShipmentItemRepository,
) {

    @Transactional
    fun checkout(buyerId: String): List<Order> {
        val cartItems = cartItemRepository.findByBuyerId(buyerId)
        if (cartItems.isEmpty()) throw IllegalStateException("Cart is empty")

        // Group by seller
        val bySeller = cartItems.groupBy { ci ->
            val item = itemRepository.findById(ci.itemId).orElseThrow()
            item.sellerId
        }

        val orders = mutableListOf<Order>()
        val processedItemIds = mutableListOf<Long>()

        for ((sellerId, items) in bySeller) {
            var totalAmount = BigDecimal.ZERO
            val orderItems = mutableListOf<OrderItem>()

            // Deduct stock + calculate totals
            for (ci in items) {
                val updated = itemRepository.deductStock(ci.itemId, ci.quantity)
                if (updated == 0) {
                    throw OptimisticLockingFailureException("Stock changed for item ${ci.itemId}")
                }
                val item = itemRepository.findById(ci.itemId).orElseThrow()
                val oi = OrderItem(itemId = ci.itemId, quantity = ci.quantity, unitPrice = item.price)
                totalAmount += item.price * BigDecimal(ci.quantity)
                orderItems.add(oi)
                processedItemIds.add(ci.itemId)
            }

            // Create Order
            val order = orderRepository.save(Order(
                buyerId = buyerId,
                sellerId = sellerId,
                totalAmount = totalAmount,
            ))

            // Save OrderItems
            orderItems.forEach { it.orderId = order.id; orderItemRepository.save(it) }

            // Auto-create Shipment (v1: full)
            val shipment = shipmentRepository.save(Shipment(orderId = order.id))
            orderItems.forEach { oi ->
                shipmentItemRepository.save(ShipmentItem(shipmentId = shipment.id, itemId = oi.itemId, quantity = oi.quantity))
            }

            orders.add(order)
        }

        // Clear cart for processed items
        cartItemRepository.deleteByBuyerIdAndItemIdIn(buyerId, processedItemIds)

        return orders
    }

    @Transactional
    fun markPaid(orderId: Long, sellerId: String): Order {
        val order = orderRepository.findById(orderId).orElseThrow()
            .also { if (it.sellerId != sellerId) throw SecurityException("Not your order") }
            .also { if (it.status != OrderStatus.PENDING_PAYMENT) throw IllegalStateException("Invalid status: ${it.status}") }

        order.status = OrderStatus.PAID
        order.paymentStatus = "paid"
        order.paidAt = java.time.Instant.now()
        return orderRepository.save(order)
    }

    @Transactional
    fun ship(orderId: Long, sellerId: String, trackingNumber: String?): Shipment {
        val order = orderRepository.findById(orderId).orElseThrow()
            .also { if (it.sellerId != sellerId) throw SecurityException("Not your order") }
            .also { if (it.status != OrderStatus.PAID) throw IllegalStateException("Order not paid") }

        order.status = OrderStatus.SHIPPING
        orderRepository.save(order)

        val shipment = shipmentRepository.findByOrderId(orderId).firstOrNull()
            ?: throw IllegalStateException("No shipment found")
        shipment.status = ShipmentStatus.SHIPPED
        shipment.trackingNumber = trackingNumber
        shipment.shippedAt = java.time.Instant.now()
        return shipmentRepository.save(shipment)
    }

    @Transactional
    fun confirmDelivered(orderId: Long, buyerId: String): Shipment {
        val order = orderRepository.findById(orderId).orElseThrow()
            .also { if (it.buyerId != buyerId) throw SecurityException("Not your order") }
            .also { if (it.status != OrderStatus.SHIPPING) throw IllegalStateException("Not shipped yet") }

        order.status = OrderStatus.DELIVERED
        orderRepository.save(order)

        val shipment = shipmentRepository.findByOrderId(orderId).firstOrNull()
            ?: throw IllegalStateException("No shipment found")
        shipment.status = ShipmentStatus.DELIVERED
        shipment.deliveredAt = java.time.Instant.now()
        return shipmentRepository.save(shipment)
    }
}

private operator fun BigDecimal.times(n: Int): BigDecimal = this.multiply(BigDecimal(n))
