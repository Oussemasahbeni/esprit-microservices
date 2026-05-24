package com.esprit.reservation.config.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static io.swagger.v3.oas.models.security.SecurityScheme.In.HEADER;
import static io.swagger.v3.oas.models.security.SecurityScheme.Type.OAUTH2;

@Configuration
class OpenApiConfig {

    @Value("${keycloak.server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Bean
    public OpenAPI customOpenAPI() {
        var authorizationUrl =
                serverUrl
                        + "/realms/"
                        + realm
                        + "/protocol/openid-connect/auth";
        var tokenUrl =
                serverUrl
                        + "/realms/"
                        + realm
                        + "/protocol/openid-connect/token";
        return new OpenAPI()
                .info(new Info()
                        .title("Reservation API")
                        .version("1.0.0")
                        .description("Reservation Service API")
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
