package com.esprit.menu.config.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI menuManagementOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Zestro Menu Management API")
                        .version("1.0")
                        .description("CRUD for dishes, categories, variants, availability and promotions."));
    }
}
