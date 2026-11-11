package br.com.vet.clinico.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;
import java.util.List;

public record RegistrarPrescricaoRequest(
        @PastOrPresent(message = "Data de emissão não pode ser futura")
        LocalDate dataEmissao,

        String orientacoesGerais,

        @NotEmpty(message = "A prescrição deve conter ao menos um item")
        List<@Valid ItemPrescricaoRequest> itens
) {
}
