package com.joycai.orderservice.repository

import com.joycai.orderservice.model.Category
import org.springframework.data.jpa.repository.JpaRepository

interface CategoryRepository : JpaRepository<Category, Long> {
    fun findByParentId(parentId: Long?): List<Category>
}
