package com.joycai.orderservice.controller

import com.joycai.orderservice.model.Category
import com.joycai.orderservice.repository.CategoryRepository
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
class CategoryController(
    private val categoryRepository: CategoryRepository,
) {

    @GetMapping("/seller/categories")
    @PreAuthorize("hasAnyRole('seller', 'admin')")
    fun listAll(): ResponseEntity<List<Map<String, Any>>> {
        val all = categoryRepository.findAll()
        val l1 = all.filter { it.parentId == null }.map { cat ->
            mapOf(
                "id" to cat.id,
                "name" to cat.name,
                "children" to all.filter { it.parentId == cat.id }.map {
                    mapOf("id" to it.id, "name" to it.name, "parentId" to it.parentId)
                },
            )
        }
        return ResponseEntity.ok(l1)
    }

    @PostMapping("/seller/categories")
    @PreAuthorize("hasAnyRole('seller', 'admin')")
    fun create(@RequestBody body: Map<String, String>): ResponseEntity<Map<String, Any>> {
        val name = body["name"] ?: return bad("name required")
        val parentId = (body["parentId"] as? String)?.toLongOrNull()
        val cat = categoryRepository.save(Category(name = name, parentId = parentId))
        return ResponseEntity.ok(mapOf("id" to cat.id, "name" to cat.name, "parentId" to (cat.parentId ?: "")))
    }

    @PutMapping("/seller/categories/{id}")
    @PreAuthorize("hasAnyRole('seller', 'admin')")
    fun update(@PathVariable id: Long, @RequestBody body: Map<String, String>): ResponseEntity<Map<String, Any>> {
        val cat = categoryRepository.findById(id).orElse(null) ?: return ResponseEntity.notFound().build()
        body["name"]?.let { cat.name = it }
        body["parentId"]?.let { cat.parentId = it.toLongOrNull() }
        categoryRepository.save(cat)
        return ResponseEntity.ok(mapOf("id" to cat.id, "name" to cat.name))
    }

    @DeleteMapping("/seller/categories/{id}")
    @PreAuthorize("hasAnyRole('seller', 'admin')")
    fun delete(@PathVariable id: Long): ResponseEntity<Map<String, String>> {
        if (!categoryRepository.existsById(id)) return ResponseEntity.notFound().build()
        categoryRepository.deleteById(id)
        return ResponseEntity.ok(mapOf("message" to "Deleted"))
    }

    private fun bad(msg: String): ResponseEntity<Map<String, Any>> = ResponseEntity.badRequest().body(mapOf("message" to msg))
}
