package com.joycai.orderservice.repository

import com.joycai.orderservice.model.Order
import com.joycai.orderservice.model.OrderStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant

interface OrderRepository : JpaRepository<Order, Long> {
    fun findByBuyerId(buyerId: String): List<Order>
    fun findBySellerId(sellerId: String): List<Order>

    @Query("SELECT o FROM Order o WHERE o.buyerId = :buyerId AND (:status IS NULL OR o.status = :status) AND o.createdAt >= :from AND (:to IS NULL OR o.createdAt <= :to) ORDER BY o.createdAt DESC")
    fun findByBuyerIdFiltered(buyerId: String, status: OrderStatus?, from: Instant, to: Instant?): List<Order>

    @Query("SELECT o FROM Order o WHERE o.sellerId = :sellerId AND (:status IS NULL OR o.status = :status) AND o.createdAt >= :from AND (:to IS NULL OR o.createdAt <= :to) ORDER BY o.createdAt DESC")
    fun findBySellerIdFiltered(sellerId: String, status: OrderStatus?, from: Instant, to: Instant?): List<Order>
}
