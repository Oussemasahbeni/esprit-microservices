package com.esprit.delivery.enums;

/**
 * Represents the real-time availability of a driver for new deliveries.
 */
public enum DriverAvailabilityStatus {

    /** Driver is online and free to receive new delivery assignments. */
    AVAILABLE,

    /** Driver is currently handling one or more deliveries. */
    BUSY,

    /** Driver is on a break and should not receive new assignments. */
    ON_BREAK,

    /** Driver is not connected / logged out of the driver app. */
    OFFLINE
}

