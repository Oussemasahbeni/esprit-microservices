package com.esprit.employee.iam.model;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RoleType {
  MANAGER("manager"),
  ADMIN("admin"),
  DELIVERY_MANAGER("delivery-manager"),
  DELIVERY_MAN("delivery-man"),
  CUSTOMER("customer");

  private final String value;

  public static Optional<RoleType> fromValue(String value) {
    for (RoleType roleType : RoleType.values()) {
      if (roleType.getValue().equalsIgnoreCase(value)) {
        return Optional.of(roleType);
      }
    }
    return Optional.empty();
  }

  public static List<String> getAllRoleNames() {
    return Stream.of(RoleType.values()).map(Enum::name).toList();
  }
}
