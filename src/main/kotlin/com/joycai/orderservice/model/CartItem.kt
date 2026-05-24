package com.joycai.orderservice.model

import jakarta.persistence.*

@Entity
@Table(name = "cart_items", uniqueConstraints = [UniqueConstraint(columnNames = ["buyer_id", "item_id"])])
class CartItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "buyer_id", nullable = false)
    var buyerId: String,

    @Column(name = "item_id", nullable = false)
    var itemId: Long,

    @Column(nullable = false)
    var quantity: Int,
)
