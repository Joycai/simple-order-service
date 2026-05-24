package com.joycai.orderservice.repository

import com.joycai.orderservice.model.Order
import org.springframework.data.jpa.repository.JpaRepository

interface OrderRepository : JpaRepository<Order, Long> {
    fun findByBuyerId(buyerId: String): List<Order>
    fun findBySellerId(sellerId: String): List<Order>
}
