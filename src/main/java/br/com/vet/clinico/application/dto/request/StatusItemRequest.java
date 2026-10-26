package br.com.vet.clinico.application.dto.request;

import br.com.vet.clinico.domain.enums.StatusItem;
import jakarta.validation.constraints.NotNull;

public record StatusItemRequest(
        @NotNull(message = "Status é obrigatório")
        StatusItem status
) {
}
