
package com.nyle.nylepay.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    
    @Bean
    public OpenAPI nylePayOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("NylePay API")
                .description("Digital Wallet API for global transactions with MPesa, bank transfers, and cryptocurrency support")
                .version("1.0.0")
                .contact(new Contact()
                    .name("NylePay Support")
                    .email("support@nylepay.com")
                    .url("https://nylepay.com"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0")))
            .components(new io.swagger.v3.oas.models.Components()
                .addSecuritySchemes("bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
