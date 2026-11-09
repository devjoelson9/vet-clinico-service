package br.com.vet.clinico.application.dto.response;

import br.com.vet.clinico.domain.enums.StatusAtendimento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AtendimentoResponse(
        Long id,
        Long agendamentoId,
        Long animalId,
        Long veterinarioId,
        StatusAtendimento status,
        BigDecimal pesoKg,
        String queixaSintomas,
        String diagnostico,
        String tratamento,
        String observacoes,
        String orientacoes,
        LocalDateTime iniciadoEm,
        LocalDateTime finalizadoEm,
        BigDecimal valorBase,
        BigDecimal valorTotal,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
}
