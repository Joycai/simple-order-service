package com.joycai.orderservice.controller

import com.joycai.orderservice.model.Item
import com.joycai.orderservice.model.ItemStatus
import com.joycai.orderservice.repository.CategoryRepository
import com.joycai.orderservice.repository.ItemRepository
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

@RestController
@RequestMapping("/api/v1")
class ItemController(
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository,
) {

    @GetMapping("/categories")
    fun categories(): ResponseEntity<List<Map<String, Any>>> {
        val all = categoryRepository.findAll()
        val l1 = all.filter { it.parentId == null }.map { cat ->
            mapOf(
                "id" to cat.id,
                "name" to cat.name,
                "children" to all.filter { it.parentId == cat.id }.map {
                    mapOf("id" to it.id, "name" to it.name)
                },
            )
        }
        return ResponseEntity.ok(l1)
    }

    @GetMapping("/items")
    fun listItems(
        @RequestParam(required = false) categoryId: Long?,
        @RequestParam(required = false) keyword: String?,
    ): ResponseEntity<List<Map<String, Any>>> {
        val items = when {
            categoryId != null -> itemRepository.findByCategoryId(categoryId)
            else -> itemRepository.findAll().filter { it.status == ItemStatus.ACTIVE }
        }.filter { it.status == ItemStatus.ACTIVE }
            .let { list ->
                if (keyword != null) list.filter {
                    it.name.contains(keyword, ignoreCase = true) ||
                    (it.description?.contains(keyword, ignoreCase = true) == true)
                } else list
            }
        return ResponseEntity.ok(items.map { it.toMap() })
    }

    @GetMapping("/items/{id}")
    fun getItem(@PathVariable id: Long): ResponseEntity<Map<String, Any>> {
        val item = itemRepository.findById(id).orElse(null)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(item.toMap())
    }

    @PostMapping("/seller/items")
    @PreAuthorize("hasRole('seller')")
    fun createItem(
        @RequestBody body: Map<String, Any>,
        @AuthenticationPrincipal sellerId: String,
    ): ResponseEntity<Map<String, Any>> {
        val item = itemRepository.save(Item(
            sellerId = sellerId,
            categoryId = (body["categoryId"] as? Number)?.toLong() ?: return bad("categoryId required"),
            name = body["name"] as? String ?: return bad("name required"),
            description = body["description"] as? String,
            brand = body["brand"] as? String,
            location = body["location"] as? String,
            price = BigDecimal(body["price"]?.toString() ?: return bad("price required")),
            quantity = (body["quantity"] as? Number)?.toInt() ?: return bad("quantity required"),
        ))
        return ResponseEntity.ok(item.toMap())
    }

    @PutMapping("/seller/items/{id}")
    @PreAuthorize("hasRole('seller')")
    fun updateItem(
        @PathVariable id: Long,
        @RequestBody body: Map<String, Any>,
        @AuthenticationPrincipal sellerId: String,
    ): ResponseEntity<Map<String, Any>> {
        val item = itemRepository.findById(id).orElse(null)
            ?: return ResponseEntity.notFound().build()
        if (item.sellerId != sellerId) return ResponseEntity.status(403).body(mapOf("message" to "Access denied"))
        (body["name"] as? String)?.let { item.name = it }
        (body["description"] as? String)?.let { item.description = it }
        (body["brand"] as? String)?.let { item.brand = it }
        (body["location"] as? String)?.let { item.location = it }
        body["price"]?.toString()?.let { item.price = BigDecimal(it) }
        (body["quantity"] as? Number)?.toInt()?.let { item.quantity = it }
        (body["status"] as? String)?.let { item.status = ItemStatus.valueOf(it.uppercase()) }
        itemRepository.save(item)
        return ResponseEntity.ok(item.toMap())
    }

    @GetMapping("/seller/items")
    @PreAuthorize("hasRole('seller')")
    fun myItems(@AuthenticationPrincipal sellerId: String): ResponseEntity<List<Map<String, Any>>> {
        return ResponseEntity.ok(itemRepository.findBySellerId(sellerId).map { it.toMap() })
    }

    private fun Item.toMap() = mapOf(
        "id" to id,
        "sellerId" to sellerId,
        "categoryId" to categoryId,
        "name" to name,
        "description" to (description ?: ""),
        "brand" to (brand ?: ""),
        "location" to (location ?: ""),
        "price" to price,
        "quantity" to quantity,
        "status" to status.name,
        "createdAt" to createdAt.toString(),
    )

    private fun bad(msg: String): ResponseEntity<Map<String, Any>> = ResponseEntity.badRequest().body(mapOf("message" to msg))
}
