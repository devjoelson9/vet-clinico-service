package br.com.vet.clinico.infrastructure.messaging;

import br.com.vet.clinico.application.event.AtendimentoEventPublisher;
import br.com.vet.clinico.application.event.AtendimentoEvento;
import br.com.vet.clinico.domain.model.Atendimento;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class RabbitAtendimentoEventPublisher implements AtendimentoEventPublisher {

    public static final String ROUTING_KEY_INICIADO = "atendimento.iniciado";
    public static final String ROUTING_KEY_FINALIZADO = "atendimento.realizado";
    public static final String ROUTING_KEY_CANCELADO = "atendimento.cancelado";

    private final RabbitTemplate rabbitTemplate;
    private final TopicExchange atendimentoExchange;
    private final ObjectMapper objectMapper;

    @Override
    public void publicarIniciado(Atendimento atendimento) {
        publicar(ROUTING_KEY_INICIADO, atendimento);
    }

    @Override
    public void publicarFinalizado(Atendimento atendimento) {
        publicar(ROUTING_KEY_FINALIZADO, atendimento);
    }

    @Override
    public void publicarCancelado(Atendimento atendimento) {
        publicar(ROUTING_KEY_CANCELADO, atendimento);
    }

    private void publicar(String routingKey, Atendimento atendimento) {
        String payload = objectMapper.writeValueAsString(AtendimentoEvento.de(atendimento));
        rabbitTemplate.convertAndSend(
                atendimentoExchange.getName(),
                routingKey,
                payload
        );
    }
}
