package br.com.vet.clinico.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AtualizarCatalogoItemRequest(
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
        String nome,

        String descricao,

        @DecimalMin(value = "0.00", message = "Valor base não pode ser negativo")
        @Digits(integer = 8, fraction = 2, message = "Valor base deve ter no máximo 8 dígitos inteiros e 2 decimais")
        BigDecimal valorBase
) {
}
