package com.esprit.delivery.service.implementation;

import com.esprit.delivery.client.feign.MenuServiceClient;
import com.esprit.delivery.client.feign.dto.DishResponse;
import com.esprit.delivery.dto.CreateOrderRequest;
import com.esprit.delivery.dto.DeliveryDtos.UpdateOrderStatusRequest;
import com.esprit.delivery.entity.Order;
import com.esprit.delivery.entity.OrderItem;
import com.esprit.delivery.enums.OrderStatus;
import com.esprit.delivery.exception.ApplicationException;
import com.esprit.delivery.repository.OrderRepository;
import com.esprit.delivery.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static com.esprit.delivery.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    /**
     * Allowed forward transitions. Anything not listed here is rejected.
     */
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(OrderStatus.class);
    /**
     * Cancellation is only allowed before the order has been physically picked up.
     */
    private static final Set<OrderStatus> CANCELLABLE_STATUSES =
            EnumSet.of(OrderStatus.PLACED, OrderStatus.PREPARING, OrderStatus.READY_FOR_PICKUP);
    private static final List<OrderStatus> TERMINAL_STATUSES =
            List.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED);

    static {
        ALLOWED_TRANSITIONS.put(OrderStatus.PLACED, EnumSet.of(OrderStatus.PREPARING, OrderStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(OrderStatus.PREPARING, EnumSet.of(OrderStatus.READY_FOR_PICKUP, OrderStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(OrderStatus.READY_FOR_PICKUP, EnumSet.of(OrderStatus.PICKED_UP, OrderStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(OrderStatus.PICKED_UP, EnumSet.of(OrderStatus.IN_TRANSIT));
        ALLOWED_TRANSITIONS.put(OrderStatus.IN_TRANSIT, EnumSet.of(OrderStatus.DELIVERED));
        ALLOWED_TRANSITIONS.put(OrderStatus.DELIVERED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED_TRANSITIONS.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
    }

    private final OrderRepository orderRepository;
    private final MenuServiceClient menuServiceClient;
//    private final DeliveryEventPublisher eventPublisher;

    @Override
    @Transactional
    public Order placeOrder(CreateOrderRequest request) {

        Order order = Order.builder()
                .customerId(request.getCustomerId())
                .deliveryAddress(request.getDeliveryAddress())
                .status(OrderStatus.PLACED)
                .items(new ArrayList<>())
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            if (itemRequest.getQuantity() == null || itemRequest.getQuantity() <= 0) {
                throw new ApplicationException(INVALID_ORDER,
                        "Quantity must be positive for dish " + itemRequest.getDishId());
            }

            // Never trust client-supplied name/price: resolve them from MS2.
            DishResponse dish = menuServiceClient.getDishById(itemRequest.getDishId());
            if (dish == null || !dish.isAvailable()) {
                throw new ApplicationException(UNAVAILABLE_DISH, "Dish not found: " + itemRequest.getDishId());
            }

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .dishId(dish.getId())
                    .dishName(dish.getName())
                    .unitPrice(dish.getPrice())
                    .quantity(itemRequest.getQuantity())
                    .notes(itemRequest.getNotes())
                    .build();

            order.getItems().add(orderItem);
            total = total.add(orderItem.getSubTotal());
        }

        order.setTotalAmount(total);

        Order saved = orderRepository.save(order);

//        eventPublisher.publishOrderPlaced(toOrderPlacedEvent(saved));
        log.info("Order {} placed for customer {} (total={})", saved.getId(), saved.getCustomerId(), saved.getTotalAmount());

        return saved;
    }

    @Override
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ApplicationException(ORDER_NOT_FOUND, "Order not found with id: " + orderId));
    }

    @Override
    public List<Order> getOrdersByCustomer(Long customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    @Override
    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    @Override
    @Transactional
    public Order updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        Order order = getOrderById(orderId);
        OrderStatus previousStatus = order.getStatus();
        OrderStatus newStatus = request.getNewStatus();

        if (newStatus == null) {
            throw new ApplicationException(
                    INVALID_ORDER,
                    "newStatus is required"
            );
        }

        Set<OrderStatus> allowedNextStatuses = ALLOWED_TRANSITIONS.getOrDefault(previousStatus, EnumSet.noneOf(OrderStatus.class));
        if (!allowedNextStatuses.contains(newStatus)) {
            throw new ApplicationException(
                    INVALID_ORDER_STATUS_TRANSITION,
                    String.format(
                            "Invalid order status transition from %s to %s",
                            previousStatus,
                            newStatus
                    )
            );
        }

        order.setStatus(newStatus);

        if (newStatus == OrderStatus.DELIVERED) {
            LocalDateTime now = LocalDateTime.now();
            order.setDeliveredAt(now);
            if (order.getAssignment() != null) {
                order.getAssignment().setCompletedAt(now);
            }
        }

        if (newStatus == OrderStatus.PICKED_UP && order.getAssignment() != null
                && order.getAssignment().getPickedUpAt() == null) {
            order.getAssignment().setPickedUpAt(LocalDateTime.now());
        }

        Order saved = orderRepository.save(order);

//        eventPublisher.publishOrderStatusChanged(OrderStatusChangedEvent.builder()
//                .orderId(saved.getId())
//                .customerId(saved.getCustomerId())
//                .previousStatus(previousStatus)
//                .newStatus(newStatus)
//                .build());

//        if (newStatus == OrderStatus.DELIVERED) {
//            eventPublisher.publishOrderDelivered(OrderDeliveredEvent.builder()
//                    .orderId(saved.getId())
//                    .customerId(saved.getCustomerId())
//                    .deliveredAt(saved.getDeliveredAt())
//                    .build());
//        }

        log.info("Order {} transitioned {} -> {}", orderId, previousStatus, newStatus);

        return saved;
    }

    @Override
    @Transactional
    public Order cancelOrder(Long orderId, String reason) {
        Order order = getOrderById(orderId);
        OrderStatus previousStatus = order.getStatus();

        if (!CANCELLABLE_STATUSES.contains(previousStatus)) {
            throw new ApplicationException(ORDER_NOT_ALLOWED, "Can't do this action");
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);

//        eventPublisher.publishOrderStatusChanged(OrderStatusChangedEvent.builder()
//                .orderId(saved.getId())
//                .customerId(saved.getCustomerId())
//                .previousStatus(previousStatus)
//                .newStatus(OrderStatus.CANCELLED)
//                .build());

        log.info("Order {} cancelled from status {} (reason: {})", orderId, previousStatus, reason);

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public void handleDishUnavailable(Long dishId) {
        List<Order> affectedOrders = orderRepository.findNonTerminalOrdersContainingDish(dishId, TERMINAL_STATUSES);

        if (affectedOrders.isEmpty()) {
            return;
        }

        log.warn("Dish {} became unavailable; flagging {} affected order(s) for review",
                dishId, affectedOrders.size());

//        for (Order order : affectedOrders) {
//            eventPublisher.publishOrderFlaggedForReview(OrderFlaggedForReviewEvent.builder()
//                    .orderId(order.getId())
//                    .customerId(order.getCustomerId())
//                    .dishId(dishId)
//                    .reason("Dish " + dishId + " is no longer available")
//                    .build());
//        }
    }

//    private OrderPlacedEvent toOrderPlacedEvent(Order order) {
//        return OrderPlacedEvent.builder()
//                .orderId(order.getId())
//                .customerId(order.getCustomerId())
//                .totalAmount(order.getTotalAmount())
//                .items(order.getItems().stream()
//                        .map(item -> OrderPlacedEvent.Item.builder()
//                                .dishId(item.getDishId())
//                                .dishName(item.getDishName())
//                                .quantity(item.getQuantity())
//                                .build())
//                        .collect(Collectors.toList()))
//                .build();
//    }
}