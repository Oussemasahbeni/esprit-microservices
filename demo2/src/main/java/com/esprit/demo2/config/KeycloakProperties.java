package com.esprit.demo2.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "keycloak")
public record KeycloakProperties(
        @NotBlank(message = "Keycloak admin client server URL must be configured") String serverUrl,
        @NotBlank(message = "Keycloak admin client realm must be configured") String realm
) {
}
