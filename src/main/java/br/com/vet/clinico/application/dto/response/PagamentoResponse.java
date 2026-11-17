
package br.com.vet.clinico.application.dto.response;

import br.com.vet.clinico.domain.enums.StatusPagamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagamentoResponse(
        Long id,
        Long atendimentoId,
        BigDecimal valor,
        StatusPagamento status,
        LocalDateTime registradoEm,
        LocalDateTime pagoEm
) {
}
