package com.esprit.delivery.entity;

import com.esprit.delivery.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregate root of the Delivery Management Service.
 *
 * Represents a customer order to be delivered. An {@link Order} owns its
 * {@link OrderItem}s and is linked to a single active {@link DeliveryAssignment}
 * (the driver currently responsible for it) and to its {@link ChatMessage} history.
 *
 * The {@code customerId} field is a foreign reference to the identity managed
 * by Keycloak / the customer's profile (no local copy of customer data is kept,
 * in line with the "each microservice owns its own data" principle).
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Keycloak subject (user id) of the customer who placed the order. */
    @Column(nullable = false)
    private Long customerId;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @Embedded
    private DeliveryAddress deliveryAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PLACED;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    /** Currently assigned driver, if any. Null while the order is unassigned. */
    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private DeliveryAssignment assignment;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /** Timestamp at which the order reached DELIVERED status. */
    private LocalDateTime deliveredAt;
}

