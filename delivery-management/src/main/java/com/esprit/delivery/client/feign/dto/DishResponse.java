package com.esprit.delivery.client.feign.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Subset of the dish representation exposed by MS2 (Menu Management Service),
 * as returned by {@code GET /dishes/{id}}. Only the fields needed by the
 * Delivery service to build an {@code OrderItem} snapshot are mapped here.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DishResponse {
    private Long id;
    private String name;
    private BigDecimal price;
    private boolean available;
}
