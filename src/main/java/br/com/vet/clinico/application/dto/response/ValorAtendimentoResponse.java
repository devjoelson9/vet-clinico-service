package br.com.vet.clinico.application.dto.response;

import java.math.BigDecimal;

public record ValorAtendimentoResponse(
        Long atendimentoId,
        BigDecimal valorBase,
        BigDecimal valorProcedimentos,
        BigDecimal valorMedicamentos,
        BigDecimal valorTotal
) {
}
