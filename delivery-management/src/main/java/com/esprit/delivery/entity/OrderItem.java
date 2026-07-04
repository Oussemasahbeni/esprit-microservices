package com.esprit.delivery.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A single line item within a delivery {@link Order}.
 *
 * Note: the dish itself is NOT owned by this microservice. Only the
 * {@code dishId} (foreign reference to MS2 - Menu Management Service) and a
 * denormalized snapshot of its name/price are stored here, so that an order's
 * content remains stable even if the dish is later renamed, repriced, or
 * deleted in the Menu service. The snapshot is fetched once via
 * {@code MenuServiceClient} at order-creation time.
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private Order order;

    /** Reference to the dish in the Menu Management Service (MS2). */
    @Column(nullable = false)
    private Long dishId;

    /** Snapshot of the dish name at order time (denormalized for history/display). */
    @Column(nullable = false)
    private String dishName;

    /** Snapshot of the unit price at order time. */
    @Column(nullable = false)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    /** Optional customer notes for this item (e.g. "no onions"). */
    private String notes;

    public BigDecimal getSubTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
