package br.com.vet.clinico.application.gateway;

public interface AgendamentoGateway {

    void validarInicioAtendimento(Long agendamentoId, Long animalId, Long veterinarioId);
}
