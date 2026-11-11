package br.com.vet.clinico.application.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PrescricaoResponse(
        Long id,
        Long atendimentoId,
        LocalDate dataEmissao,
        String orientacoesGerais,
        LocalDateTime criadoEm,
        List<ItemPrescricaoResponse> itens
) {
}
