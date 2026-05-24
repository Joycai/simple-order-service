package com.joycai.orderservice.model

import jakarta.persistence.*

@Entity
@Table(name = "categories")
class Category(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(nullable = false, length = 50)
    var name: String,

    @Column(name = "parent_id")
    var parentId: Long? = null,
)
