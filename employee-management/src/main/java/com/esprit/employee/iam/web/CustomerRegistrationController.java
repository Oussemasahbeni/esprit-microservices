package com.esprit.employee.iam.web;

import com.esprit.employee.iam.dto.CustomerRegistrationRequest;
import com.esprit.employee.iam.dto.IdentityUserResponse;
import com.esprit.employee.iam.model.IdentityUser;
import com.esprit.employee.iam.model.RoleType;
import com.esprit.employee.iam.service.IdentityGateway;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/iam/customers")
@RequiredArgsConstructor
public class CustomerRegistrationController {

  private final IdentityGateway identityGateway;

  @PostMapping("/register")
  public ResponseEntity<IdentityUserResponse> registerCustomer(
      @Valid @RequestBody CustomerRegistrationRequest request) {

    IdentityUser user =
        identityGateway.create(
            IdentityUser.builder()
                .username(request.email())
                .email(request.email())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .roles(List.of(RoleType.CUSTOMER))
                .enabled(true)
                .build(),
            List.of(),
            true);

    identityGateway.setPassword(UUID.fromString(user.getId()), request.password(), false);

    return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(user));
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
