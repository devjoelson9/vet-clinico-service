package br.com.vet.clinico.infrastructure.integration;

import br.com.vet.clinico.application.exception.BusinessException;
import br.com.vet.clinico.application.exception.ResourceNotFoundException;
import br.com.vet.clinico.application.gateway.AgendamentoGateway;
import br.com.vet.clinico.infrastructure.security.AuthorizationHeaderProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.Objects;

@Component
public class AgendamentoClient implements AgendamentoGateway {

    private final RestClient restClient;
    private final AuthorizationHeaderProvider authorizationHeaderProvider;

    public AgendamentoClient(
            RestClient.Builder restClientBuilder,
            @Value("${agendamento.base-url}") String baseUrl,
            AuthorizationHeaderProvider authorizationHeaderProvider
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.authorizationHeaderProvider = authorizationHeaderProvider;
    }

    @Override
    public Long validarInicioAtendimento(Long agendamentoId, Long animalId, Long veterinarioEsperado) {
        AgendamentoInfo agendamento = buscar(agendamentoId);

        if (!"AGUARDANDO_ATENDIMENTO".equals(agendamento.status())) {
            throw new BusinessException(
                    "O agendamento precisa estar com check-in realizado (AGUARDANDO_ATENDIMENTO) para iniciar o atendimento"
            );
        }
        if (!Objects.equals(agendamento.animalId(), animalId)) {
            throw new BusinessException("O animal informado não pertence ao agendamento");
        }
        if (agendamento.veterinarioId() == null) {
            throw new BusinessException("O agendamento não possui veterinário responsável");
        }
        if (veterinarioEsperado != null && !Objects.equals(agendamento.veterinarioId(), veterinarioEsperado)) {
            throw new AccessDeniedException("O agendamento pertence a outro veterinário");
        }
        return agendamento.veterinarioId();
    }

    private AgendamentoInfo buscar(Long agendamentoId) {
        try {
            return restClient.get()
                    .uri("/api/v1/agendamentos/{id}", agendamentoId)
                    .headers(headers -> {
                        String authorization = authorizationHeaderProvider.current();
                        if (authorization != null) {
                            headers.set(HttpHeaders.AUTHORIZATION, authorization);
                        }
                    })
                    .retrieve()
                    .body(AgendamentoInfo.class);
        } catch (org.springframework.web.client.RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            if (status == 404) {
                throw new ResourceNotFoundException("Agendamento não encontrado: " + agendamentoId);
            }
            if (status == 401 || status == 403) {
                throw new AccessDeniedException("Acesso negado pelo serviço de agendamento");
            }
            throw new BusinessException("Falha ao consultar o serviço de agendamento");
        } catch (ResourceAccessException exception) {
            throw new BusinessException("Serviço de agendamento indisponível");
        }
    }
}
