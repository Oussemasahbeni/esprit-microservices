package com.esprit.employee.config.keycloak;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class KeycloakConnectionValidator
        implements ConstraintValidator<KeycloakConnectionValid, KeycloakProperties> {

    private static final Logger log = LoggerFactory.getLogger(KeycloakConnectionValidator.class);

    @Override
    public void initialize(KeycloakConnectionValid constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(KeycloakProperties properties, ConstraintValidatorContext context) {
        Keycloak keycloak = null;
        try {
            log.info(
                    "Attempting to validate Keycloak admin client connection to server: {}, realm: {}",
                    properties.serverUrl(),
                    properties.realm());

            keycloak =
                    KeycloakBuilder.builder()
                            .serverUrl(properties.serverUrl())
                            .realm(properties.realm())
                            .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                            .clientId(properties.clientId())
                            .clientSecret(properties.clientSecret())
                            .build();

            // Validate both connectivity and client credentials without requiring
            // broad admin permissions like server-info access.
            keycloak.tokenManager().getAccessTokenString();

            log.info(
                    "Successfully validated Keycloak admin client connection for realm '{}'.",
                    properties.realm());
            return true;
        } catch (WebApplicationException | ProcessingException e) {
            log.warn("Keycloak admin client connection validation failed: {}", e.getMessage());
            context.disableDefaultConstraintViolation();
            context
                    .buildConstraintViolationWithTemplate(
                            String.format(
                                    "Failed to connect to Keycloak or validate credentials: %s. URL: %s, Realm: %s, ClientID: %s. Check Keycloak server status and configuration.",
                                    e.getMessage(),
                                    properties.serverUrl(),
                                    properties.realm(),
                                    properties.clientId()))
                    .addConstraintViolation();
            return false;
        } catch (Exception e) {
            log.error(
                    "Unexpected error during Keycloak admin client connection validation: {}",
                    e.getMessage(),
                    e);
            context.disableDefaultConstraintViolation();
            context
                    .buildConstraintViolationWithTemplate(
                            String.format("Unexpected error during Keycloak validation: %s", e.getMessage()))
                    .addConstraintViolation();
            return false;
        } finally {
            if (keycloak != null) {
                keycloak.close();
            }
        }
    }
}
