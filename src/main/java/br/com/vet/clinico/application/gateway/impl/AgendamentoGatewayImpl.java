
package br.com.vet.clinico.application.gateway.impl;

import br.com.vet.clinico.application.exception.BusinessException;
import br.com.vet.clinico.application.gateway.AgendamentoGateway;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class AgendamentoGatewayImpl implements AgendamentoGateway {

    private final RestClient.Builder restClientBuilder;

    @Value("${agendamento.service.url:}")
    private String agendamentoServiceUrl;

    @Override
    public void validarInicioAtendimento(
            Long agendamentoId,
            Long animalId,
            Long veterinarioId
    ) {
        AgendamentoResponse response = restClient()
                .get()
                .uri("/api/v1/agendamentos/{id}", agendamentoId)
                .header(HttpHeaders.AUTHORIZATION, obterToken())
                .retrieve()
                .body(AgendamentoResponse.class);

        if (response == null) {
            throw new BusinessException(
                    "Não foi possível consultar o agendamento informado"
            );
        }

        if (!Objects.equals(response.animalId(), animalId)) {
            throw new BusinessException(
                    "O animal do atendimento não corresponde ao agendamento"
            );
        }

        if (!Objects.equals(response.veterinarioId(), veterinarioId)) {
            throw new BusinessException(
                    "O veterinário não corresponde ao agendamento"
            );
        }

        if (!"AGUARDANDO_ATENDIMENTO".equals(response.status())) {
            throw new BusinessException(
                    "O agendamento não está aguardando atendimento"
            );
        }
    }

    @Override
    public void alterarStatus(Long agendamentoId, String novoStatus) {
        restClient()
                .patch()
                .uri("/api/v1/agendamentos/{id}/status", agendamentoId)
                .header(HttpHeaders.AUTHORIZATION, obterToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("status", novoStatus))
                .retrieve()
                .toBodilessEntity();
    }

    private RestClient restClient() {
        if (agendamentoServiceUrl == null
                || agendamentoServiceUrl.isBlank()) {
            throw new BusinessException(
                    "A URL do serviço de agendamentos não foi configurada"
            );
        }

        return restClientBuilder
                .baseUrl(agendamentoServiceUrl.replaceAll("/+$", ""))
                .build();
    }

    private String obterToken() {
        if (!(RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes attributes)) {
            throw new BusinessException(
                    "Não foi possível obter a requisição autenticada"
            );
        }

        HttpServletRequest request = attributes.getRequest();
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BusinessException(
                    "Token de autenticação não encontrado"
            );
        }

        return authorization;
    }

    private record AgendamentoResponse(
            Long id,
            Long animalId,
            Long veterinarioId,
            String status
    ) {
    }
}
