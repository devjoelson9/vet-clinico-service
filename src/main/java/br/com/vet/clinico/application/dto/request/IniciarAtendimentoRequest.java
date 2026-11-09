package br.com.vet.clinico.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record IniciarAtendimentoRequest(
        @NotNull(message = "Agendamento é obrigatório")
        @Positive(message = "Agendamento inválido")
        Long agendamentoId,

        @NotNull(message = "Animal é obrigatório")
        @Positive(message = "Animal inválido")
        Long animalId,

        @DecimalMin(value = "0.01", message = "Peso deve ser maior que zero")
        @Digits(integer = 3, fraction = 2, message = "Peso deve ter no máximo 3 dígitos inteiros e 2 decimais")
        BigDecimal pesoKg
) {
}
