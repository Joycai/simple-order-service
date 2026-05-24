package com.joycai.orderservice.repository

import com.joycai.orderservice.model.Shipment
import org.springframework.data.jpa.repository.JpaRepository

interface ShipmentRepository : JpaRepository<Shipment, Long> {
    fun findByOrderId(orderId: Long): List<Shipment>
}
