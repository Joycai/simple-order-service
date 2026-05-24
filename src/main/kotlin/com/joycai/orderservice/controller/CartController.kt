package com.joycai.orderservice.controller

import com.joycai.orderservice.model.CartItem
import com.joycai.orderservice.repository.CartItemRepository
import com.joycai.orderservice.repository.ItemRepository
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/cart")
@PreAuthorize("hasRole('buyer')")
class CartController(
    private val cartItemRepository: CartItemRepository,
    private val itemRepository: ItemRepository,
) {

    @GetMapping
    fun getCart(@AuthenticationPrincipal buyerId: String): ResponseEntity<List<Map<String, Any>>> {
        val items = cartItemRepository.findByBuyerId(buyerId).map { ci ->
            val item = itemRepository.findById(ci.itemId).orElse(null)
            mapOf(
                "id" to ci.id,
                "itemId" to ci.itemId,
                "quantity" to ci.quantity,
                "name" to (item?.name ?: ""),
                "price" to (item?.price?.toString() ?: "0"),
                "itemQuantity" to (item?.quantity ?: 0),
            )
        }
        return ResponseEntity.ok(items)
    }

    @PostMapping("/items")
    fun addItem(
        @RequestBody body: Map<String, Any>,
        @AuthenticationPrincipal buyerId: String,
    ): ResponseEntity<Map<String, Any>> {
        val itemId = (body["itemId"] as? Number)?.toLong() ?: return bad("itemId required")
        val qty = (body["quantity"] as? Number)?.toInt() ?: 1
        val item = itemRepository.findById(itemId).orElse(null)
            ?: return ResponseEntity.badRequest().body(mapOf("message" to "Item not found"))
        if (item.quantity < qty) return ResponseEntity.badRequest().body(mapOf("message" to "Insufficient stock"))

        val existing = cartItemRepository.findByBuyerIdAndItemId(buyerId, itemId)
        if (existing != null) {
            existing.quantity += qty
            cartItemRepository.save(existing)
        } else {
            cartItemRepository.save(CartItem(buyerId = buyerId, itemId = itemId, quantity = qty))
        }
        return ResponseEntity.ok(mapOf("message" to "Added to cart"))
    }

    @PutMapping("/items/{id}")
    fun updateQuantity(
        @PathVariable id: Long,
        @RequestBody body: Map<String, Any>,
    ): ResponseEntity<Map<String, Any>> {
        val ci = cartItemRepository.findById(id).orElse(null)
            ?: return ResponseEntity.notFound().build()
        val qty = (body["quantity"] as? Number)?.toInt() ?: return bad("quantity required")
        if (qty <= 0) { cartItemRepository.delete(ci); return ResponseEntity.ok(mapOf("message" to "Removed")) }
        ci.quantity = qty
        cartItemRepository.save(ci)
        return ResponseEntity.ok(mapOf("message" to "Updated"))
    }

    @DeleteMapping("/items/{id}")
    fun removeItem(@PathVariable id: Long): ResponseEntity<Map<String, String>> {
        cartItemRepository.deleteById(id)
        return ResponseEntity.ok(mapOf("message" to "Removed"))
    }

    private fun bad(msg: String) = ResponseEntity.badRequest().body(mapOf("message" to msg))
}
