package com.verimetra.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI verimetraOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("VeriMetra API")
                        .description("Evidence-Based Regulatory Compliance Verification and Incremental Revalidation System")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("VeriMetra Team")));
    }
}
