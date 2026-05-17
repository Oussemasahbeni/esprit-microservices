package com.esprit.demo2.config.openapi;

import com.esprit.demo2.config.KeycloakProperties;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static io.swagger.v3.oas.models.security.SecurityScheme.In.HEADER;
import static io.swagger.v3.oas.models.security.SecurityScheme.Type.OAUTH2;

@Configuration
@RequiredArgsConstructor
class OpenApiConfig {

    private final KeycloakProperties keycloakProperties;

    @Bean
    public OpenAPI customOpenAPI() {
        var authorizationUrl =
                keycloakProperties.serverUrl()
                        + "/realms/"
                        + keycloakProperties.realm()
                        + "/protocol/openid-connect/auth";
        var tokenUrl =
                keycloakProperties.serverUrl()
                        + "/realms/"
                        + keycloakProperties.realm()
                        + "/protocol/openid-connect/token";
        return new OpenAPI()
                .info(new Info()
                        .title("Demo2 API")
                        .version("1.0.0")
                        .description("Demo1 Service API")
                )
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Gateway")
                ))
                .addSecurityItem(new SecurityRequirement().addList("keycloak"))
                .schemaRequirement(
                        "keycloak",
                        new SecurityScheme()
                                .type(OAUTH2)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(HEADER)
                                .flows(
                                        new OAuthFlows()
                                                .authorizationCode(
                                                        new OAuthFlow()
                                                                .authorizationUrl(authorizationUrl)
                                                                .tokenUrl(tokenUrl)
                                                )
                                )
                );
    }
}
