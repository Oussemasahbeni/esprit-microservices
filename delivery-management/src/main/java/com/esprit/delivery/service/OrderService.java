package com.esprit.delivery.service;

import com.esprit.delivery.dto.CreateOrderRequest;
import com.esprit.delivery.dto.DeliveryDtos.UpdateOrderStatusRequest;
import com.esprit.delivery.entity.Order;
import com.esprit.delivery.enums.OrderStatus;

import java.util.List;

/**
 * Use cases related to the creation and lifecycle of delivery orders.
 * <p>
 * Implementations are expected to:
 * - Validate dish references and prices through {@code MenuServiceClient} (Feign).
 * - Persist orders via {@code OrderRepository}.
 * - Publish domain events through {@code DeliveryEventPublisher} (RabbitMQ)
 * on creation and on every status transition.
 */
public interface OrderService {

    /**
     * Creates a new order (maps to {@code POST /orders}).
     * Resolves dish name/price/availability via the Menu service, computes
     * the total, persists the order with status PLACED, and publishes an
     * {@code order.placed} event for the kitchen to start preparation.
     */
    Order placeOrder(CreateOrderRequest request);

    /**
     * Retrieves a single order by id, throwing if not found.
     */
    Order getOrderById(Long orderId);

    /**
     * Retrieves the full order history for a given customer.
     */
    List<Order> getOrdersByCustomer(Long customerId);

    /**
     * Retrieves all orders currently in a given status (e.g. for dashboards).
     */
    List<Order> getOrdersByStatus(OrderStatus status);

    /**
     * Updates the status of an order (maps to {@code PATCH /orders/{id}/status}).
     * Enforces valid transitions (e.g. cannot go from DELIVERED back to PREPARING),
     * stamps {@code deliveredAt} when transitioning to DELIVERED, and publishes
     * the corresponding {@code order.status.changed} / {@code order.delivered} events.
     */
    Order updateOrderStatus(Long orderId, UpdateOrderStatusRequest request);

    /**
     * Cancels an order, allowed only while it has not yet been picked up.
     */
    Order cancelOrder(Long orderId, String reason);

    /**
     * Reacts to a {@code dish.unavailable} event consumed from MS2: flags any
     * non-terminal order containing the dish for manual review/customer notice.
     */
    void handleDishUnavailable(Long dishId);
}
