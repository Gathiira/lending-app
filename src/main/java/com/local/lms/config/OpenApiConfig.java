package com.local.lms.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI lendingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Loan Management System")
                        .description("RESTful API for Lms — manages loan products, customers, loan disbursement, repayments, and notifications.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Gathiira")
                                .email("gathiiramwangi@gmail.com"))
                        .license(new License().name("MIT")));
    }
}
