package com.esprit.delivery.client.feign.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Client-side mirror of the employee/IAM service's identity response
 * (GET /api/iam/users/{id}). Used to validate a customer's Keycloak identity
 * before caching them locally — see {@code CustomerServiceImpl}.
 * <p>
 * Kept deliberately narrow: only the fields delivery-ms actually needs.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IdentityUserResponse(
        String id,
        String username,
        String firstName,
        String lastName,
        String email,
        Boolean enabled,
        List<String> roles
) {
    public boolean isActive() {
        return Boolean.TRUE.equals(enabled);
    }

    public boolean isCustomer() {
        return roles != null && roles.contains("CUSTOMER");
    }
}