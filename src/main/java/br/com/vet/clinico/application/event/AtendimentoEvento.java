package br.com.vet.clinico.application.event;

import br.com.vet.clinico.domain.model.Atendimento;

import java.time.LocalDateTime;

public record AtendimentoEvento(
        Long atendimentoId,
        Long agendamentoId,
        Long animalId,
        Long veterinarioId,
        String status,
        LocalDateTime iniciadoEm,
        LocalDateTime finalizadoEm,
        LocalDateTime ocorridoEm
) {

    public static AtendimentoEvento de(Atendimento atendimento) {
        return new AtendimentoEvento(
                atendimento.getId(),
                atendimento.getAgendamentoId(),
                atendimento.getAnimalId(),
                atendimento.getVeterinarioId(),
                atendimento.getStatus().name(),
                atendimento.getIniciadoEm(),
                atendimento.getFinalizadoEm(),
                LocalDateTime.now()
        );
    }
}
