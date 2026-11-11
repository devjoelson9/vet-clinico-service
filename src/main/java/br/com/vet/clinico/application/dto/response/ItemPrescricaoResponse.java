package br.com.vet.clinico.application.dto.response;

import java.time.LocalDateTime;

public record ItemPrescricaoResponse(
        Long id,
        Long prescricaoId,
        Long medicamentoId,
        String nomeMedicamento,
        String dosagemConcentracao,
        String posologia,
        String duracaoTratamento,
        String viaAdministracao,
        LocalDateTime criadoEm
) {
}
