package com.joycai.orderservice.model

import jakarta.persistence.*
import java.time.Instant

enum class ShipmentStatus { PREPARING, SHIPPED, DELIVERED }

@Entity
@Table(name = "shipments")
class Shipment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "order_id", nullable = false)
    var orderId: Long,

    @Column(name = "tracking_number", length = 100)
    var trackingNumber: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var status: ShipmentStatus = ShipmentStatus.PREPARING,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "shipped_at")
    var shippedAt: Instant? = null,

    @Column(name = "delivered_at")
    var deliveredAt: Instant? = null,
)
