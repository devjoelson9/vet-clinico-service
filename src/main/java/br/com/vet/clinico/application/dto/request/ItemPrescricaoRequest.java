package br.com.vet.clinico.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ItemPrescricaoRequest(
        @NotNull(message = "Medicamento é obrigatório")
        @Positive(message = "Medicamento inválido")
        Long medicamentoId,

        @NotBlank(message = "Dosagem ou concentração é obrigatória")
        @Size(max = 100, message = "Dosagem ou concentração deve ter no máximo 100 caracteres")
        String dosagemConcentracao,

        @NotBlank(message = "Posologia é obrigatória")
        @Size(max = 255, message = "Posologia deve ter no máximo 255 caracteres")
        String posologia,

        @Size(max = 50, message = "Duração do tratamento deve ter no máximo 50 caracteres")
        String duracaoTratamento,

        @Size(max = 50, message = "Via de administração deve ter no máximo 50 caracteres")
        String viaAdministracao
) {
}
