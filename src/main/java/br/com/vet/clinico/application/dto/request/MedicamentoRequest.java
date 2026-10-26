package br.com.vet.clinico.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MedicamentoRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
        String nome,

        String descricao,

        @NotNull(message = "Valor base é obrigatório")
        @DecimalMin(value = "0.00", message = "Valor base não pode ser negativo")
        @Digits(integer = 8, fraction = 2, message = "Valor base deve ter no máximo 8 dígitos inteiros e 2 decimais")
        BigDecimal valorBase
) {
}
