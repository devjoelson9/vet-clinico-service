package br.com.vet.clinico.application.mapper;

import br.com.vet.clinico.application.dto.response.ItemAtendimentoResponse;
import br.com.vet.clinico.domain.enums.TipoItemAtendimento;
import br.com.vet.clinico.domain.model.ItemAtendimento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class ItemAtendimentoMapper {

    public ItemAtendimento toEntity(
            Long atendimentoId,
            TipoItemAtendimento tipo,
            Long referenciaId,
            String nomeItem,
            BigDecimal valorUnitario,
            BigDecimal quantidade
    ) {
        ItemAtendimento item = new ItemAtendimento();
        item.setAtendimentoId(atendimentoId);
        if (tipo == TipoItemAtendimento.CATALOGO_ITEM) {
            item.setCatalogoItemId(referenciaId);
        } else {
            item.setMedicamentoId(referenciaId);
        }
        item.setNomeItem(nomeItem);
        item.setQuantidade(quantidade);
        item.setValorUnitario(valorUnitario);
        item.setValorTotal(calcularValorTotal(valorUnitario, quantidade));
        return item;
    }

    public ItemAtendimentoResponse toResponse(ItemAtendimento item) {
        TipoItemAtendimento tipo = item.getCatalogoItemId() != null
                ? TipoItemAtendimento.CATALOGO_ITEM
                : TipoItemAtendimento.MEDICAMENTO;

        return new ItemAtendimentoResponse(
                item.getId(),
                item.getAtendimentoId(),
                tipo,
                item.getCatalogoItemId(),
                item.getMedicamentoId(),
                item.getNomeItem(),
                item.getQuantidade(),
                item.getValorUnitario(),
                item.getValorTotal(),
                item.getCriadoEm()
        );
    }

    private BigDecimal calcularValorTotal(BigDecimal valorUnitario, BigDecimal quantidade) {
        return valorUnitario.multiply(quantidade).setScale(2, RoundingMode.HALF_UP);
    }
}
