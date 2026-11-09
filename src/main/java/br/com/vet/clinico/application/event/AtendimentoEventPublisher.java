package br.com.vet.clinico.application.event;

import br.com.vet.clinico.domain.model.Atendimento;

public interface AtendimentoEventPublisher {

    void publicarIniciado(Atendimento atendimento);

    void publicarFinalizado(Atendimento atendimento);

    void publicarCancelado(Atendimento atendimento);
}
