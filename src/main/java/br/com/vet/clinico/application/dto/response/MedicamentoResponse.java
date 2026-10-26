package br.com.vet.clinico.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MedicamentoResponse(
        Long id,
        String nome,
        String descricao,
        BigDecimal valorBase,
        String status,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
}
