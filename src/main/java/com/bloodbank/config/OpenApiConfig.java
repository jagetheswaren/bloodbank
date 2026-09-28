package com.bloodbank.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bloodBankOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BloodBank Inventory & Donor Eligibility API")
                        .description("REST API for donor eligibility, donations, blood-unit inventory, expiry tracking and blood issuing.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("BloodBank Administration")
                                .email("admin@bloodbank.local"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
