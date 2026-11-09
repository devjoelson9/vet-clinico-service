package br.com.vet.clinico.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HistoricoClinicoResponse(
        Long atendimentoId,
        Long agendamentoId,
        Long animalId,
        Long veterinarioId,
        BigDecimal pesoKg,
        String queixaSintomas,
        String diagnostico,
        String tratamento,
        String observacoes,
        String orientacoes,
        LocalDateTime iniciadoEm,
        LocalDateTime finalizadoEm
) {
}
