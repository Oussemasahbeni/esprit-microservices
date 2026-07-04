package com.esprit.delivery.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Join entity linking exactly one {@link Order} to the {@link Driver}
 * responsible for delivering it.
 *
 * Kept as its own entity (rather than a simple FK on Order) so that the full
 * assignment history can be preserved even if a driver is later reassigned
 * (e.g. {@code DriverAssignmentService#reassignDriver}), by closing the
 * current assignment and creating a new one.
 */
@Entity
@Table(name = "delivery_assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    /** Set when the driver marks the order as picked up from the restaurant. */
    private LocalDateTime pickedUpAt;

    /** Set when the order is marked as delivered. */
    private LocalDateTime completedAt;

    /** True while this assignment is the active one for the order. */
    @Builder.Default
    private boolean active = true;
}

