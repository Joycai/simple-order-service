package com.joycai.orderservice.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

enum class ItemStatus { ACTIVE, SOLD_OUT, HIDDEN }

@Entity
@Table(name = "items")
class Item(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "seller_id", nullable = false)
    var sellerId: String,

    @Column(name = "category_id", nullable = false)
    var categoryId: Long,

    @Column(nullable = false, length = 200)
    var name: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(length = 100)
    var brand: String? = null,

    @Column(length = 100)
    var location: String? = null,

    @Column(nullable = false, precision = 10, scale = 2)
    var price: BigDecimal,

    @Column(nullable = false)
    var quantity: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var status: ItemStatus = ItemStatus.ACTIVE,

    @Version
    var version: Long = 0,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
)
