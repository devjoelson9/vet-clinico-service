package br.com.vet.clinico.infrastructure.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ATENDIMENTO_EXCHANGE = "vet.events";

    @Bean
    public TopicExchange atendimentoExchange() {
        return new TopicExchange(ATENDIMENTO_EXCHANGE, true, false);
    }

    /**
     * Garante que as exchanges/queues declaradas como beans sejam criadas no
     * broker assim que a aplicação conectar (mesmo sem consumidores ativos).
     */
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.setAutoStartup(true);
        return rabbitAdmin;
    }
}
