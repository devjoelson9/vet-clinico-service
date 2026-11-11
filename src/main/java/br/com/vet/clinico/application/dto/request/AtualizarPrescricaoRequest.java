package br.com.vet.clinico.application.dto.request;

import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

public record AtualizarPrescricaoRequest(
        @PastOrPresent(message = "Data de emissão não pode ser futura")
        LocalDate dataEmissao,

        String orientacoesGerais
) {
}
