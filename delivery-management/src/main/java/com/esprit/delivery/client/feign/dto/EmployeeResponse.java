package com.esprit.delivery.client.feign.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Employee representation exposed by MS1 (Employee Management Service), as
 * returned by {@code GET /api/employees/{id}}. Used to validate that an
 * employee exists, is ACTIVE, and holds the DELIVERY_MAN role before being
 * registered as a {@code Driver} in this service.
 * <p>
 * {@code status} and {@code role} are kept as plain {@code String} rather
 * than enums: MS1 owns that value space, and deserializing straight into a
 * local enum would blow up (like the old primitive {@code active} field did)
 * the moment MS1 adds a new status/role we don't know about yet. Use
 * {@link #isActive()} / {@link #isDeliveryMan()} instead of comparing the
 * raw strings everywhere.
 * <p>
 * {@code @JsonIgnoreProperties(ignoreUnknown = true)} so this only needs to
 * track the fields we actually use, without breaking if MS1 adds more.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmployeeResponse {

    private Long id;
    private String keycloakUserId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;

    /**
     * e.g. "DELIVERY_MAN", "ADMIN", "MANAGER"...
     */
    private String role;

    private Long restaurantId;

    /**
     * e.g. "ACTIVE", "INACTIVE"...
     */
    private String status;

    private String position;
    private String contractType;
    private LocalDate hireDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    public boolean isDeliveryMan() {
        return "DELIVERY_MAN".equalsIgnoreCase(role);
    }
}