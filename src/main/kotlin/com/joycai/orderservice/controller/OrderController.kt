package com.joycai.orderservice.controller

import com.joycai.orderservice.model.Order
import com.joycai.orderservice.model.OrderStatus
import com.joycai.orderservice.repository.*
import com.joycai.orderservice.service.OrderService
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
class OrderController(
    private val orderService: OrderService,
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val shipmentRepository: ShipmentRepository,
    private val shipmentItemRepository: ShipmentItemRepository,
    private val itemRepository: ItemRepository,
) {

    // ── Buyer ──────────────────────────────────────────────────────

    @PostMapping("/orders")
    fun checkout(@AuthenticationPrincipal buyerId: String): ResponseEntity<Map<String, Any>> {
        return try {
            val orders = orderService.checkout(buyerId)
            ResponseEntity.ok(mapOf("orders" to orders.map { it.toMap() }))
        } catch (e: OptimisticLockingFailureException) {
            ResponseEntity.status(409).body(mapOf("message" to "Stock changed, please refresh and retry"))
        } catch (e: IllegalStateException) {
            ResponseEntity.badRequest().body(mapOf("message" to e.message!!))
        }
    }

    @GetMapping("/orders")
    fun myOrders(@AuthenticationPrincipal buyerId: String): ResponseEntity<List<Map<String, Any>>> {
        return ResponseEntity.ok(orderRepository.findByBuyerId(buyerId).map { it.toMap() })
    }

    @GetMapping("/orders/{id}")
    fun getOrder(@PathVariable id: Long): ResponseEntity<Map<String, Any>> {
        val order = orderRepository.findById(id).orElse(null)
            ?: return ResponseEntity.notFound().build()
        val items = orderItemRepository.findByOrderId(id).map { oi ->
            val item = itemRepository.findById(oi.itemId).orElse(null)
            mapOf(
                "id" to oi.id,
                "itemId" to oi.itemId,
                "name" to (item?.name ?: ""),
                "quantity" to oi.quantity,
                "unitPrice" to oi.unitPrice,
            )
        }
        val shipments = shipmentRepository.findByOrderId(id).map { s ->
            mapOf(
                "id" to s.id,
                "status" to s.status.name,
                "trackingNumber" to (s.trackingNumber ?: ""),
                "shippedAt" to (s.shippedAt?.toString() ?: ""),
            )
        }
        val data = order.toMap() + mapOf("items" to items, "shipments" to shipments)
        return ResponseEntity.ok(data)
    }

    @PostMapping("/orders/{id}/deliver")
    fun confirmDelivered(@PathVariable id: Long, @AuthenticationPrincipal buyerId: String): ResponseEntity<Map<String, Any>> {
        return try {
            val shipment = orderService.confirmDelivered(id, buyerId)
            ResponseEntity.ok(mapOf("message" to "Delivered", "shipment" to shipment.id))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("message" to e.message!!))
        }
    }

    // ── Seller ─────────────────────────────────────────────────────

    @GetMapping("/seller/orders")
    fun sellerOrders(@AuthenticationPrincipal sellerId: String): ResponseEntity<List<Map<String, Any>>> {
        return ResponseEntity.ok(orderRepository.findBySellerId(sellerId).map { it.toMap() })
    }

    @PostMapping("/seller/orders/{id}/pay")
    fun markPaid(@PathVariable id: Long, @AuthenticationPrincipal sellerId: String): ResponseEntity<Map<String, Any>> {
        return try {
            val order = orderService.markPaid(id, sellerId)
            ResponseEntity.ok(order.toMap())
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("message" to e.message!!))
        }
    }

    @PostMapping("/seller/orders/{id}/ship")
    fun ship(
        @PathVariable id: Long,
        @RequestBody body: Map<String, Any>,
        @AuthenticationPrincipal sellerId: String,
    ): ResponseEntity<Map<String, Any>> {
        return try {
            val tracking = body["trackingNumber"] as? String
            val shipment = orderService.ship(id, sellerId, tracking)
            ResponseEntity.ok(mapOf(
                "id" to shipment.id,
                "status" to shipment.status.name,
                "trackingNumber" to (shipment.trackingNumber ?: ""),
            ))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("message" to e.message!!))
        }
    }

    // ── helpers ────────────────────────────────────────────────────

    private fun Order.toMap() = mapOf(
        "id" to id,
        "buyerId" to buyerId,
        "sellerId" to sellerId,
        "status" to status.name,
        "paymentMethod" to paymentMethod,
        "paymentStatus" to paymentStatus,
        "totalAmount" to totalAmount,
        "createdAt" to createdAt.toString(),
        "paidAt" to (paidAt?.toString() ?: ""),
    )
}
