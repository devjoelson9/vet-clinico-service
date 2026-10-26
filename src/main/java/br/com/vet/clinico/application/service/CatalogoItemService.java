package br.com.vet.clinico.application.service;

import br.com.vet.clinico.application.dto.request.AtualizarCatalogoItemRequest;
import br.com.vet.clinico.application.dto.request.CatalogoItemRequest;
import br.com.vet.clinico.application.dto.response.CatalogoItemResponse;
import br.com.vet.clinico.application.exception.ConflictException;
import br.com.vet.clinico.application.exception.ResourceNotFoundException;
import br.com.vet.clinico.application.mapper.CatalogoItemMapper;
import br.com.vet.clinico.domain.enums.StatusItem;
import br.com.vet.clinico.domain.enums.TipoItemCatalogo;
import br.com.vet.clinico.domain.model.CatalogoItem;
import br.com.vet.clinico.domain.repository.CatalogoItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogoItemService {

    private final CatalogoItemRepository repository;
    private final CatalogoItemMapper mapper;

    @Transactional
    public CatalogoItemResponse cadastrar(CatalogoItemRequest request) {
        String nome = request.nome().trim();
        if (repository.existsByTipoAndNomeIgnoreCaseAndDeletadoEmIsNull(request.tipo(), nome)) {
            throw new ConflictException("Já existe um item do tipo " + request.tipo() + " com o nome informado");
        }

        CatalogoItem item = mapper.toEntity(request);
        return mapper.toResponse(repository.save(item));
    }

    @Transactional(readOnly = true)
    public Page<CatalogoItemResponse> listar(TipoItemCatalogo tipo, StatusItem status, String nome, Pageable pageable) {
        Specification<CatalogoItem> spec = (root, query, cb) -> cb.isNull(root.get("deletadoEm"));

        if (tipo != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("tipo"), tipo));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (nome != null && !nome.isBlank()) {
            String termo = "%" + nome.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("nome")), termo));
        }

        return repository.findAll(spec, pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CatalogoItemResponse buscarPorId(Long id) {
        return mapper.toResponse(buscarItemAtivo(id));
    }

    @Transactional(readOnly = true)
    public List<CatalogoItemResponse> listarAtivos(TipoItemCatalogo tipo) {
        List<CatalogoItem> itens = tipo == null
                ? repository.findByDeletadoEmIsNullAndStatusOrderByNomeAsc(StatusItem.ATIVO)
                : repository.findByDeletadoEmIsNullAndStatusAndTipoOrderByNomeAsc(StatusItem.ATIVO, tipo);

        return itens.stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public CatalogoItemResponse atualizar(Long id, AtualizarCatalogoItemRequest request) {
        CatalogoItem item = buscarItemAtivo(id);

        if (request.nome() != null && !request.nome().isBlank()) {
            String nome = request.nome().trim();
            if (repository.existsByTipoAndNomeIgnoreCaseAndDeletadoEmIsNullAndIdNot(item.getTipo(), nome, id)) {
                throw new ConflictException("Já existe um item do tipo " + item.getTipo() + " com o nome informado");
            }
        }

        mapper.updateEntity(item, request);
        return mapper.toResponse(repository.save(item));
    }

    @Transactional
    public CatalogoItemResponse alterarStatus(Long id, StatusItem status) {
        CatalogoItem item = buscarItemAtivo(id);
        item.setStatus(status);
        return mapper.toResponse(repository.save(item));
    }

    @Transactional
    public void excluir(Long id) {
        CatalogoItem item = buscarItemAtivo(id);
        item.setDeletadoEm(LocalDateTime.now());
        item.setStatus(StatusItem.INATIVO);
        repository.save(item);
    }

    private CatalogoItem buscarItemAtivo(Long id) {
        return repository.findByIdAndDeletadoEmIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item de catálogo não encontrado: " + id));
    }
}
