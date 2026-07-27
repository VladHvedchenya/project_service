package faang.school.projectservice.config.openApi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI projectServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CorporationX - Project Service API")
                        .version("1.0.0")
                        .description("RESTful API для управления проектами, стадиями (Stages), " +
                                "задачами и автоматизированными инвайтами участников команды.")
                        .contact(new Contact()
                                .name("Vlad Hvedchenya")
                                .email("hvdvlad@gmail.com   "))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://springdoc.org")));
    }
}