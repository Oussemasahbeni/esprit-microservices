package com.esprit.employee.config.keycloak;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "keycloak")
@KeycloakConnectionValid
public record KeycloakProperties(
        @NotBlank(message = "Keycloak admin client server URL must be configured") String serverUrl,
        @NotBlank(message = "Keycloak admin client realm must be configured") String realm,
        @NotBlank(message = "Keycloak admin client ID must be configured") String clientId,
        @NotBlank(message = "Keycloak admin client secret must be configured") String clientSecret,
        String frontendClientId
) {
}
