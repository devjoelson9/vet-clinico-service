package br.com.vet.clinico.infrastructure.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AgendamentoInfo(
        Long id,
        Long animalId,
        Long veterinarioId,
        String status
) {
}
