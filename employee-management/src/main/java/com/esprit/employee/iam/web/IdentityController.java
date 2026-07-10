package com.esprit.employee.iam.web;

import static com.esprit.employee.exception.ErrorCode.USER_NOT_FOUND;

import com.esprit.employee.exception.ApplicationException;
import com.esprit.employee.iam.dto.IdentityUserResponse;
import com.esprit.employee.iam.model.IdentityUser;
import com.esprit.employee.iam.service.IdentityGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/iam/users")
@RequiredArgsConstructor
public class IdentityController {

  private final IdentityGateway identityGateway;

  @GetMapping("/{id}")
  public IdentityUserResponse getById(@PathVariable String id) {
    IdentityUser user =
        identityGateway
            .findById(id)
            .orElseThrow(() -> new ApplicationException(USER_NOT_FOUND, "User not found: " + id));
    return toResponse(user);
  }

  private IdentityUserResponse toResponse(IdentityUser user) {
    return new IdentityUserResponse(
        user.getId(),
        user.getUsername(),
        user.getFirstName(),
        user.getLastName(),
        user.getEmail(),
        user.getEnabled(),
        user.getRoles());
  }
}
