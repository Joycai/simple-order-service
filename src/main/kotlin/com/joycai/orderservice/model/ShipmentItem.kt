package com.joycai.orderservice.model

import jakarta.persistence.*

@Entity
@Table(name = "shipment_items")
class ShipmentItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "shipment_id", nullable = false)
    var shipmentId: Long,

    @Column(name = "item_id", nullable = false)
    var itemId: Long,

    @Column(nullable = false)
    var quantity: Int,
)
