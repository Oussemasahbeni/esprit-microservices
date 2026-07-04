package com.esprit.delivery.controller;

import com.esprit.delivery.dto.CreateOrderRequest;
import com.esprit.delivery.dto.DeliveryDtos.UpdateOrderStatusRequest;
import com.esprit.delivery.entity.Order;
import com.esprit.delivery.enums.OrderStatus;
import com.esprit.delivery.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Place a new order.
     */
    @PostMapping
    public Order placeOrder(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.placeOrder(request);
    }

    /**
     * Get an order by its ID.
     */
    @GetMapping("/{orderId}")
    public Order getOrderById(@PathVariable Long orderId) {
        return orderService.getOrderById(orderId);
    }

    /**
     * Get all orders for a customer.
     */
    @GetMapping("/customer/{customerId}")
    public List<Order> getOrdersByCustomer(@PathVariable Long customerId) {
        return orderService.getOrdersByCustomer(customerId);
    }

    /**
     * Get all orders with a given status.
     */
    @GetMapping("/status/{status}")
    public List<Order> getOrdersByStatus(@PathVariable OrderStatus status) {
        return orderService.getOrdersByStatus(status);
    }

    /**
     * Update an order status.
     */
    @PatchMapping("/{orderId}/status")
    public Order updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {

        return orderService.updateOrderStatus(orderId, request);
    }

    /**
     * Cancel an order.
     */
    @PatchMapping("/{orderId}/cancel")
    public Order cancelOrder(
            @PathVariable Long orderId,
            @RequestParam(required = false) String reason) {

        return orderService.cancelOrder(orderId, reason);
    }

    /**
     * Internal endpoint called when a dish becomes unavailable.
     * Can later be secured or replaced with an event listener.
     */
    @PostMapping("/internal/dishes/{dishId}/unavailable")
    public void handleDishUnavailable(@PathVariable Long dishId) {
        orderService.handleDishUnavailable(dishId);
    }
}
