package com.joycai.orderservice.repository

import com.joycai.orderservice.model.CartItem
import org.springframework.data.jpa.repository.JpaRepository

interface CartItemRepository : JpaRepository<CartItem, Long> {
    fun findByBuyerId(buyerId: String): List<CartItem>
    fun findByBuyerIdAndItemId(buyerId: String, itemId: Long): CartItem?
    fun deleteByBuyerIdAndItemIdIn(buyerId: String, itemIds: List<Long>)
}
