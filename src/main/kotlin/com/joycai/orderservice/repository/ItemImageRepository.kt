package com.joycai.orderservice.repository

import com.joycai.orderservice.model.ItemImage
import org.springframework.data.jpa.repository.JpaRepository

interface ItemImageRepository : JpaRepository<ItemImage, Long> {
    fun findByItemIdOrderBySortOrder(itemId: Long): List<ItemImage>
    fun deleteByItemId(itemId: Long)
}
