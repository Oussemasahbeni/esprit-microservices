package com.esprit.employee.iam.dto;

import com.esprit.employee.iam.model.RoleType;
import java.util.List;

public record IdentityUserResponse(
    String id,
    String username,
    String firstName,
    String lastName,
    String email,
    Boolean enabled,
    List<RoleType> roles) {
  public boolean isActive() {
    return Boolean.TRUE.equals(enabled);
  }

  public boolean isCustomer() {
    return roles != null && roles.contains(RoleType.CUSTOMER);
  }
}
