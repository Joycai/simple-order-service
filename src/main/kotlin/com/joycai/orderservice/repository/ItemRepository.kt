package com.joycai.orderservice.repository

import com.joycai.orderservice.model.Item
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface ItemRepository : JpaRepository<Item, Long> {
    fun findBySellerId(sellerId: String): List<Item>
    fun findByCategoryId(categoryId: Long): List<Item>

    @Modifying
    @Query("UPDATE Item i SET i.quantity = i.quantity - :qty WHERE i.id = :id AND i.quantity >= :qty")
    fun deductStock(id: Long, qty: Int): Int
}
