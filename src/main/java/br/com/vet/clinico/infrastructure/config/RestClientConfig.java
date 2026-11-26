package br.com.vet.clinico.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * O Spring Boot 4 não disponibiliza um RestClient.Builder apenas com o
 * starter webmvc. Este bean é usado pelos clientes HTTP entre serviços.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
