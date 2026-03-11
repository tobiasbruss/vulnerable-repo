package com.vulnbookstore.controller;

import com.vulnbookstore.model.Order;
import com.vulnbookstore.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for Order management endpoints.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Order>> getOrdersByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
    }

    /**
     * Create a new order.
     * Request body: { "userId": 1, "bookId": 2, "quantity": 3 }
     */
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> request) {
        try {
            Long userId   = Long.valueOf(request.get("userId").toString());
            Long bookId   = Long.valueOf(request.get("bookId").toString());
            int  quantity = Integer.parseInt(request.get("quantity").toString());

            Order order = orderService.createOrder(userId, bookId, quantity);
            return ResponseEntity.status(HttpStatus.CREATED).body(order);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update the status of an order.
     * Request body: { "status": "CONFIRMED" }
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        String newStatus = request.get("status");
        return orderService.updateOrderStatus(id, newStatus)
                .map(order -> ResponseEntity.ok((Object) order))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Cancel an order.
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id) {
        if (orderService.cancelOrder(id)) {
            return ResponseEntity.ok(Map.of("message", "Order cancelled successfully"));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        if (orderService.deleteOrder(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
