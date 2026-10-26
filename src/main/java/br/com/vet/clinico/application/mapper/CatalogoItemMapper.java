package br.com.vet.clinico.application.mapper;

import br.com.vet.clinico.application.dto.request.AtualizarCatalogoItemRequest;
import br.com.vet.clinico.application.dto.request.CatalogoItemRequest;
import br.com.vet.clinico.application.dto.response.CatalogoItemResponse;
import br.com.vet.clinico.domain.enums.StatusItem;
import br.com.vet.clinico.domain.model.CatalogoItem;
import org.springframework.stereotype.Component;

@Component
public class CatalogoItemMapper {

    public CatalogoItem toEntity(CatalogoItemRequest request) {
        CatalogoItem item = new CatalogoItem();
        item.setTipo(request.tipo());
        item.setNome(request.nome().trim());
        item.setDescricao(normalizarDescricao(request.descricao()));
        item.setValorBase(request.valorBase());
        item.setStatus(StatusItem.ATIVO);
        return item;
    }

    public void updateEntity(CatalogoItem item, AtualizarCatalogoItemRequest request) {
        if (request.nome() != null && !request.nome().isBlank()) {
            item.setNome(request.nome().trim());
        }
        if (request.descricao() != null) {
            item.setDescricao(normalizarDescricao(request.descricao()));
        }
        if (request.valorBase() != null) {
            item.setValorBase(request.valorBase());
        }
    }

    public CatalogoItemResponse toResponse(CatalogoItem item) {
        return new CatalogoItemResponse(
                item.getId(),
                item.getTipo(),
                item.getNome(),
                item.getDescricao(),
                item.getValorBase(),
                item.getStatus().name(),
                item.getCriadoEm(),
                item.getAtualizadoEm()
        );
    }

    private String normalizarDescricao(String descricao) {
        if (descricao == null) {
            return null;
        }
        String normalizada = descricao.trim();
        return normalizada.isEmpty() ? null : normalizada;
    }
}
