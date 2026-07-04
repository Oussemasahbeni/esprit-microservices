package com.esprit.delivery.enums;

/**
 * Represents the lifecycle of a delivery order, from the moment it is placed
 * by a customer until it is delivered, cancelled, or failed.
 * <p>
 * Typical happy-path transition:
 * PLACED -> CONFIRMED -> ASSIGNED -> PICKED_UP -> IN_TRANSIT -> DELIVERED
 */
public enum OrderStatus {

    /**
     * Order has just been created by the customer, not yet validated.
     */
    PLACED,

    /**
     * Order has been validated (items available, payment authorized, etc.).
     */
    CONFIRMED,

    /**
     * Kitchen has started preparing the order.
     */
    PREPARING,

    /**
     * Kitchen has completed the order waiting for the driver to pick it up
     */
    READY_FOR_PICKUP,

    /**
     * A driver has been assigned to the order but has not picked it up yet.
     */
    ASSIGNED,

    /**
     * The driver has collected the order from the restaurant.
     */
    PICKED_UP,

    /**
     * The driver is on the way to the customer.
     */
    IN_TRANSIT,

    /**
     * The order has been successfully delivered to the customer.
     */
    DELIVERED,

    /**
     * The order was cancelled (by customer, restaurant, or system).
     */
    CANCELLED,

    /**
     * The delivery could not be completed (e.g. customer unreachable).
     */
    FAILED
}

