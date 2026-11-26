
package br.com.vet.clinico.application.gateway;

public interface AgendamentoGateway {

    /**
     * Valida se o atendimento pode ser iniciado e retorna o id do veterinário
     * responsável pelo agendamento.
     *
     * @param veterinarioEsperado id do veterinário esperado; {@code null}
     *                           quando o usuário é administrador e o vínculo
     *                           deve ser obtido do próprio agendamento
     * @return id do veterinário responsável pelo agendamento
     */
    Long validarInicioAtendimento(Long agendamentoId, Long animalId, Long veterinarioEsperado);

    /**
     * Altera o status do agendamento vinculado ao atendimento.
     */
    void alterarStatus(Long agendamentoId, String novoStatus);
}
