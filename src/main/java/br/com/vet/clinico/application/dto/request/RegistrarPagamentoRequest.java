
package br.com.vet.clinico.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RegistrarPagamentoRequest(

        @NotNull(message = "Valor do pagamento é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
        @Digits(integer = 8, fraction = 2,
                message = "Valor deve ter no máximo 8 dígitos inteiros e 2 decimais")
        BigDecimal valor

) {
}
