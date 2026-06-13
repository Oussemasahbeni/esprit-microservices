package com.esprit.employee.config.keycloak;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = KeycloakConnectionValidator.class)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface KeycloakConnectionValid {
    String message() default "Invalid Keycloak connection details or Keycloak server is unreachable.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
