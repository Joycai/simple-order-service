package com.joycai.orderservice.controller

import com.joycai.orderservice.model.ItemImage
import com.joycai.orderservice.repository.ItemImageRepository
import com.joycai.orderservice.repository.ItemRepository
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.security.MessageDigest

@RestController
@RequestMapping("/api/v1")
class ItemImageController(
    private val itemImageRepository: ItemImageRepository,
    private val itemRepository: ItemRepository,
) {

    @GetMapping("/items/{itemId}/images")
    fun listImages(@PathVariable itemId: Long): ResponseEntity<List<Map<String, Any>>> {
        val images = itemImageRepository.findByItemIdOrderBySortOrder(itemId).map {
            mapOf("id" to it.id, "itemId" to it.itemId, "data" to it.data, "sortOrder" to it.sortOrder)
        }
        return ResponseEntity.ok(images)
    }

    @PostMapping("/seller/items/{itemId}/images")
    fun uploadImage(
        @PathVariable itemId: Long,
        @RequestBody body: Map<String, Any>,
        @AuthenticationPrincipal sellerId: String,
    ): ResponseEntity<Map<String, Any>> {
        val item = itemRepository.findById(itemId).orElse(null)
            ?: return ResponseEntity.notFound().build()
        if (item.sellerId != sellerId) return ResponseEntity.status(403).body(mapOf("message" to "Access denied"))

        val data = body["data"] as? String ?: return ResponseEntity.badRequest().body(mapOf("message" to "data required"))
        if (data.length > 1_500_000) return ResponseEntity.badRequest().body(mapOf("message" to "Image too large (max ~1MB base64)"))

        val hash = MessageDigest.getInstance("SHA-256").digest(data.toByteArray()).joinToString("") { "%02x".format(it) }

        // Skip if same hash already exists for this item
        val existing = itemImageRepository.findByItemIdOrderBySortOrder(itemId)
        if (existing.any { it.contentHash == hash }) {
            return ResponseEntity.ok(mapOf("id" to existing.first { it.contentHash == hash }.id, "skipped" to true))
        }

        val count = existing.size
        val image = itemImageRepository.save(ItemImage(itemId = itemId, data = data, contentHash = hash, sortOrder = count))
        return ResponseEntity.ok(mapOf("id" to image.id, "sortOrder" to image.sortOrder))
    }

    @PutMapping("/seller/items/{itemId}/images/reorder")
    fun reorderImages(
        @PathVariable itemId: Long,
        @RequestBody body: Map<String, Any>,
        @AuthenticationPrincipal sellerId: String,
    ): ResponseEntity<Map<String, String>> {
        val item = itemRepository.findById(itemId).orElse(null) ?: return ResponseEntity.notFound().build()
        if (item.sellerId != sellerId) return ResponseEntity.status(403).body(mapOf("message" to "Access denied"))

        @Suppress("UNCHECKED_CAST")
        val order = body["ids"] as? List<Int> ?: return ResponseEntity.badRequest().body(mapOf("message" to "ids required"))
        order.forEachIndexed { index, id ->
            val img = itemImageRepository.findById(id.toLong()).orElse(null)
            if (img != null && img.itemId == itemId) {
                img.sortOrder = index
                itemImageRepository.save(img)
            }
        }
        return ResponseEntity.ok(mapOf("message" to "Reordered"))
    }

    @DeleteMapping("/seller/item-images/{id}")
    fun deleteImage(@PathVariable id: Long, @AuthenticationPrincipal sellerId: String): ResponseEntity<Map<String, String>> {
        val img = itemImageRepository.findById(id).orElse(null)
            ?: return ResponseEntity.notFound().build()
        val item = itemRepository.findById(img.itemId).orElse(null)
        if (item?.sellerId != sellerId) return ResponseEntity.status(403).body(mapOf("message" to "Access denied"))
        itemImageRepository.delete(img)
        return ResponseEntity.ok(mapOf("message" to "Deleted"))
    }
}
