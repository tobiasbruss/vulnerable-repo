package com.vulnbookstore.service;

import com.vulnbookstore.model.Book;
import com.vulnbookstore.model.Order;
import com.vulnbookstore.repository.BookRepository;
import com.vulnbookstore.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for Order management operations.
 * Handles order creation, retrieval, and status updates.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final BookRepository bookRepository;

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    public List<Order> getOrdersByStatus(String status) {
        return orderRepository.findByStatus(status);
    }

    /**
     * Create a new order for a given book and quantity.
     */
    @Transactional
    public Order createOrder(Long userId, Long bookId, int quantity) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found: " + bookId));

        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        int priceCents = book.getPrice().multiply(BigDecimal.valueOf(100)).intValue();
        int totalCents = priceCents * quantity;
        BigDecimal totalPrice = BigDecimal.valueOf(totalCents).divide(BigDecimal.valueOf(100));

        Order order = new Order();
        order.setUserId(userId);
        order.setBookId(bookId);
        order.setQuantity(quantity);
        order.setTotalPrice(totalPrice);
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());

        logger.info("Creating order for user {} — book {} x{}", userId, bookId, quantity);
        return orderRepository.save(order);
    }

    /**
     * Update the status of an existing order.
     */
    @Transactional
    public Optional<Order> updateOrderStatus(Long orderId, String newStatus) {
        return orderRepository.findById(orderId).map(order -> {
            order.setStatus(newStatus);
            logger.info("Order {} status updated to {}", orderId, newStatus);
            return orderRepository.save(order);
        });
    }

    /**
     * Cancel an order by setting its status to CANCELLED.
     */
    @Transactional
    public boolean cancelOrder(Long orderId) {
        return orderRepository.findById(orderId).map(order -> {
            order.setStatus("CANCELLED");
            orderRepository.save(order);
            logger.info("Order {} cancelled", orderId);
            return true;
        }).orElse(false);
    }

    @Transactional
    public boolean deleteOrder(Long id) {
        if (orderRepository.existsById(id)) {
            orderRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
