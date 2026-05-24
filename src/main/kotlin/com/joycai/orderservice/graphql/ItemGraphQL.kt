package com.joycai.orderservice.graphql

import com.joycai.orderservice.model.*
import com.joycai.orderservice.repository.*
import com.joycai.orderservice.service.OrderService
import org.springframework.graphql.data.method.annotation.*
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import java.math.BigDecimal

@Controller
class ItemGraphQL(
    private val itemRepository: ItemRepository,
    private val itemImageRepository: ItemImageRepository,
    private val categoryRepository: CategoryRepository,
    private val cartItemRepository: CartItemRepository,
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val shipmentRepository: ShipmentRepository,
    private val orderService: OrderService,
) {

    // ── Queries ──────────────────────────────────────────────────

    @QueryMapping
    fun items(@Argument categoryId: Long?, @Argument keyword: String?): List<Item> {
        val all = categoryId?.let { itemRepository.findByCategoryId(it) }
            ?: itemRepository.findAll()
        return all.filter { it.status == ItemStatus.ACTIVE }.let { list ->
            if (!keyword.isNullOrBlank()) list.filter {
                it.name.contains(keyword, ignoreCase = true) ||
                it.description?.contains(keyword, ignoreCase = true) == true
            } else list
        }
    }

    @QueryMapping
    fun item(@Argument id: Long): Item? = itemRepository.findById(id).orElse(null)

    @QueryMapping
    fun categories(): List<Category> = categoryRepository.findAll()

    @QueryMapping
    fun sellerItems(@AuthenticationPrincipal sellerId: String): List<Item> =
        itemRepository.findBySellerId(sellerId)

    @QueryMapping
    fun cart(@AuthenticationPrincipal buyerId: String): List<CartItem> =
        cartItemRepository.findByBuyerId(buyerId)

    @QueryMapping
    fun buyerOrders(@AuthenticationPrincipal buyerId: String): List<Order> =
        orderRepository.findByBuyerId(buyerId)

    @QueryMapping
    fun sellerOrders(@AuthenticationPrincipal sellerId: String): List<Order> =
        orderRepository.findBySellerId(sellerId)

    @QueryMapping
    fun order(@Argument id: Long): Order? = orderRepository.findById(id).orElse(null)

    // ── Mutations ────────────────────────────────────────────────

    @MutationMapping
    fun createItem(@Argument input: Map<String, Any>, @AuthenticationPrincipal sellerId: String): Item {
        return itemRepository.save(Item(
            sellerId = sellerId,
            categoryId = (input["categoryId"] as? Number)?.toLong() ?: 0,
            name = input["name"] as? String ?: "",
            description = input["description"] as? String,
            brand = input["brand"] as? String,
            location = input["location"] as? String,
            price = BigDecimal(input["price"]?.toString() ?: "0"),
            quantity = (input["quantity"] as? Number)?.toInt() ?: 1,
        ))
    }

    @MutationMapping
    fun addToCart(@Argument itemId: Long, @Argument quantity: Int, @AuthenticationPrincipal buyerId: String): CartItem {
        val existing = cartItemRepository.findByBuyerIdAndItemId(buyerId, itemId)
        return if (existing != null) {
            existing.quantity += quantity; cartItemRepository.save(existing)
        } else {
            cartItemRepository.save(CartItem(buyerId = buyerId, itemId = itemId, quantity = quantity))
        }
    }

    @MutationMapping
    fun checkout(@AuthenticationPrincipal buyerId: String): List<Order> = orderService.checkout(buyerId)

    @MutationMapping
    fun markPaid(@Argument orderId: Long, @AuthenticationPrincipal sellerId: String): Order =
        orderService.markPaid(orderId, sellerId)

    @MutationMapping
    fun ship(@Argument orderId: Long, @Argument trackingNumber: String?, @AuthenticationPrincipal sellerId: String): Shipment =
        orderService.ship(orderId, sellerId, trackingNumber)

    // ── Field resolvers (N+1 prevention via DataLoader built-in) ─

    @SchemaMapping(typeName = "Item", field = "category")
    fun category(item: Item): Category? = categoryRepository.findById(item.categoryId).orElse(null)

    @SchemaMapping(typeName = "Item", field = "images")
    fun images(item: Item): List<ItemImage> = itemImageRepository.findByItemIdOrderBySortOrder(item.id)

    @SchemaMapping(typeName = "Item", field = "thumbnail")
    fun thumbnail(item: Item): String? = itemImageRepository.findByItemIdOrderBySortOrder(item.id).firstOrNull()?.data

    @SchemaMapping(typeName = "Category", field = "children")
    fun children(cat: Category): List<Category> = categoryRepository.findByParentId(cat.id)

    @SchemaMapping(typeName = "CartItem", field = "item")
    fun cartItem(ci: CartItem): Item? = itemRepository.findById(ci.itemId).orElse(null)

    @SchemaMapping(typeName = "Order", field = "items")
    fun orderItems(order: Order): List<OrderItem> = orderItemRepository.findByOrderId(order.id)

    @SchemaMapping(typeName = "Order", field = "shipments")
    fun shipments(order: Order): List<Shipment> = shipmentRepository.findByOrderId(order.id)
}
