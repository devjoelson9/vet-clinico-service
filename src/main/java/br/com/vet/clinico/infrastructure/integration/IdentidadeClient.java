package br.com.vet.clinico.infrastructure.integration;

import br.com.vet.clinico.application.exception.BusinessException;
import br.com.vet.clinico.infrastructure.security.AuthorizationHeaderProvider;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Optional;

@Component
public class IdentidadeClient {

    private final RestClient restClient;
    private final AuthorizationHeaderProvider authorizationHeaderProvider;

    public IdentidadeClient(
            RestClient.Builder restClientBuilder,
            @Value("${identidade.base-url}") String baseUrl,
            AuthorizationHeaderProvider authorizationHeaderProvider
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.authorizationHeaderProvider = authorizationHeaderProvider;
    }

    public Optional<Long> buscarVeterinarioId(Long usuarioId) {
        try {
            VeterinarioInfo veterinario = restClient.get()
                    .uri("/api/v1/veterinarios/usuario/{usuarioId}", usuarioId)
                    .headers(this::autenticar)
                    .retrieve()
                    .body(VeterinarioInfo.class);

            return Optional.ofNullable(veterinario).map(VeterinarioInfo::id);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            throw new BusinessException("Falha ao consultar o serviço de identidade");
        } catch (ResourceAccessException exception) {
            throw new BusinessException("Serviço de identidade indisponível");
        }
    }

    /**
     * Valida que o animal informado pertence ao tutor autenticado. A regra de
     * posse fica no serviço de identidade; o clínico apenas a consulta.
     */
    public void validarAnimalDoTutor(Long animalId) {
        try {
            restClient.get()
                    .uri("/api/v1/tutores/me/animais/{animalId}", animalId)
                    .headers(this::autenticar)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            if (status == 404 || status == 403) {
                throw new AccessDeniedException("Animal não pertence ao tutor autenticado");
            }
            if (status == 401) {
                throw new AccessDeniedException("Sessão inválida para consultar o animal");
            }
            throw new BusinessException("Falha ao validar o animal no serviço de identidade");
        } catch (ResourceAccessException exception) {
            throw new BusinessException("Serviço de identidade indisponível");
        }
    }

    private void autenticar(HttpHeaders headers) {
        String authorization = authorizationHeaderProvider.current();
        if (authorization != null) {
            headers.set(HttpHeaders.AUTHORIZATION, authorization);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VeterinarioInfo(Long id, String nomeCompleto, String crmv, String especialidade, String status) {
    }
}
