package com.esprit.employee.iam.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum RoleType {
    MANAGER("manager"),
    ADMIN("admin"),
    DELIVERY_MANAGER("delivery-manager");


    private final String value;


    public static Optional<RoleType> fromString(String roleStr) {
        for (RoleType roleType : RoleType.values()) {
            if (roleType.name().equalsIgnoreCase(roleStr)) {
                return Optional.of(roleType);
            }
        }
        return Optional.empty();
    }

    public static List<String> getAllRoleNames() {
        return Stream.of(RoleType.values()).map(Enum::name).toList();
    }
}
