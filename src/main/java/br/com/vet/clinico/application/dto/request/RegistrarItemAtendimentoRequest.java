package br.com.vet.clinico.application.dto.request;

import br.com.vet.clinico.domain.enums.TipoItemAtendimento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record RegistrarItemAtendimentoRequest(
        @NotNull(message = "Tipo do item é obrigatório")
        TipoItemAtendimento tipo,

        @NotNull(message = "Item de origem é obrigatório")
        @Positive(message = "Item de origem deve ser válido")
        Long referenciaId,

        @NotNull(message = "Quantidade é obrigatória")
        @DecimalMin(value = "0.01", message = "Quantidade deve ser maior que zero")
        @Digits(integer = 6, fraction = 2, message = "Quantidade deve ter no máximo 6 dígitos inteiros e 2 decimais")
        BigDecimal quantidade
) {
}
