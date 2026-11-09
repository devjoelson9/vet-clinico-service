package br.com.vet.clinico.infrastructure.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ATENDIMENTO_EXCHANGE = "vet.events";

    @Bean
    public TopicExchange atendimentoExchange() {
        return new TopicExchange(ATENDIMENTO_EXCHANGE, true, false);
    }
}
