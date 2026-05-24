package com.joycai.orderservice.model

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "order_items")
class OrderItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "order_id", nullable = false)
    var orderId: Long = 0,

    @Column(name = "item_id", nullable = false)
    var itemId: Long,

    @Column(nullable = false)
    var quantity: Int,

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    var unitPrice: BigDecimal,
)
