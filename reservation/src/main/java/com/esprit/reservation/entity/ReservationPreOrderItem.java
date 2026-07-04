package com.esprit.reservation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * A dish the customer pre-ordered at booking time, snapshotting name/price so the
 * record stays meaningful even if the dish is later renamed, repriced, or removed
 * from the menu-management catalog.
 */
@Entity
@Table(name = "reservation_pre_order_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationPreOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Column(name = "menu_dish_id", nullable = false)
    private Long menuDishId;

    @Column(name = "dish_name", nullable = false, length = 140)
    private String dishName;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "still_available", nullable = false)
    @Builder.Default
    private boolean stillAvailable = true;
}
