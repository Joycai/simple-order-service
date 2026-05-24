package com.joycai.orderservice.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

enum class OrderStatus { PENDING_PAYMENT, PAID, SHIPPING, PARTIALLY_SHIPPED, DELIVERED, DONE, CANCELLED }

@Entity
@Table(name = "orders")
class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "buyer_id", nullable = false)
    var buyerId: String,

    @Column(name = "seller_id", nullable = false)
    var sellerId: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: OrderStatus = OrderStatus.PENDING_PAYMENT,

    @Column(name = "payment_method", nullable = false, length = 20)
    var paymentMethod: String = "offline_pay",

    @Column(name = "payment_status", nullable = false, length = 16)
    var paymentStatus: String = "unpaid",

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    var totalAmount: BigDecimal,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "paid_at")
    var paidAt: Instant? = null,

    @Version
    var version: Long = 0,
)
