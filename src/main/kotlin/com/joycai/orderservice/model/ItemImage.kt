package com.joycai.orderservice.model

import jakarta.persistence.*

@Entity
@Table(name = "item_images")
class ItemImage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "item_id", nullable = false)
    var itemId: Long,

    @Column(nullable = false, columnDefinition = "TEXT")
    var data: String,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,
)
