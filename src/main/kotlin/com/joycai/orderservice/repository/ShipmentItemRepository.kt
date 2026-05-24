package com.joycai.orderservice.repository

import com.joycai.orderservice.model.ShipmentItem
import org.springframework.data.jpa.repository.JpaRepository

interface ShipmentItemRepository : JpaRepository<ShipmentItem, Long> {
    fun findByShipmentId(shipmentId: Long): List<ShipmentItem>
}
