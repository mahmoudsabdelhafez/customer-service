package com.bank.customer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Document-level metadata for the generated OpenAPI 3 spec. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customerServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Customer Service API")
                .version("v1")
                .description("Management of bank customers. Customer ids are 7-digit numbers "
                        + "and form the first 7 digits of every account number."));
    }
}
