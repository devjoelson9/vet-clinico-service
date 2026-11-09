package br.com.vet.clinico.application.dto.response;

import br.com.vet.clinico.domain.enums.TipoItemAtendimento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ItemAtendimentoResponse(
        Long id,
        Long atendimentoId,
        TipoItemAtendimento tipo,
        Long catalogoItemId,
        Long medicamentoId,
        String nomeItem,
        BigDecimal quantidade,
        BigDecimal valorUnitario,
        BigDecimal valorTotal,
        LocalDateTime criadoEm
) {
}
