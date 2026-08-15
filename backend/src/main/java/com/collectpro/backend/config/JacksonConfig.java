package com.collectpro.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * spring-boot-starter-jackson (Spring Boot 4) n'auto-configure que le
 * JsonMapper Jackson 3 (tools.jackson.*). Certains services du projet
 * (ex. CollecteAttachmentService) utilisent encore l'API Jackson 2
 * classique (com.fasterxml.jackson.databind.ObjectMapper) ; on fournit
 * donc explicitement ce bean en attendant une éventuelle migration vers
 * Jackson 3.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
