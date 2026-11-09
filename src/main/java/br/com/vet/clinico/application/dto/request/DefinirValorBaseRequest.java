package br.com.vet.clinico.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DefinirValorBaseRequest(
        @NotNull(message = "Serviço é obrigatório")
        @Positive(message = "Serviço inválido")
        Long servicoId
) {
}
