package br.com.vet.clinico.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;

public record AtualizarAtendimentoRequest(
        @DecimalMin(value = "0.01", message = "Peso deve ser maior que zero")
        @Digits(integer = 3, fraction = 2, message = "Peso deve ter no máximo 3 dígitos inteiros e 2 decimais")
        BigDecimal pesoKg,

        String queixaSintomas,

        String diagnostico,

        String tratamento,

        String observacoes,

        String orientacoes
) {
}
