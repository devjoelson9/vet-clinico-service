package br.com.vet.clinico.application.dto.response;

import br.com.vet.clinico.domain.enums.TipoItemCatalogo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CatalogoItemResponse(
        Long id,
        TipoItemCatalogo tipo,
        String nome,
        String descricao,
        BigDecimal valorBase,
        String status,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
}
