package br.com.vet.clinico.application.service;

import br.com.vet.clinico.application.dto.request.RegistrarItemAtendimentoRequest;
import br.com.vet.clinico.application.dto.response.ItemAtendimentoResponse;
import br.com.vet.clinico.application.exception.BusinessException;
import br.com.vet.clinico.application.exception.ResourceNotFoundException;
import br.com.vet.clinico.application.mapper.ItemAtendimentoMapper;
import br.com.vet.clinico.domain.enums.StatusItem;
import br.com.vet.clinico.domain.enums.StatusAtendimento;
import br.com.vet.clinico.domain.enums.TipoItemAtendimento;
import br.com.vet.clinico.domain.model.Atendimento;
import br.com.vet.clinico.domain.model.CatalogoItem;
import br.com.vet.clinico.domain.model.ItemAtendimento;
import br.com.vet.clinico.domain.model.Medicamento;
import br.com.vet.clinico.domain.repository.AtendimentoRepository;
import br.com.vet.clinico.domain.repository.CatalogoItemRepository;
import br.com.vet.clinico.domain.repository.ItemAtendimentoRepository;
import br.com.vet.clinico.domain.repository.MedicamentoRepository;
import br.com.vet.clinico.infrastructure.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ItemAtendimentoService {

    private final ItemAtendimentoRepository repository;
    private final AtendimentoRepository atendimentoRepository;
    private final CatalogoItemRepository catalogoItemRepository;
    private final MedicamentoRepository medicamentoRepository;
    private final ItemAtendimentoMapper mapper;

    @Transactional
    public ItemAtendimentoResponse registrar(
            Long atendimentoId,
            RegistrarItemAtendimentoRequest request,
            UsuarioAutenticado usuario
    ) {
        buscarAtendimentoEmAndamento(atendimentoId, usuario);

        OrigemItem origem = resolverOrigem(request.tipo(), request.referenciaId());
        ItemAtendimento item = mapper.toEntity(
                atendimentoId,
                request.tipo(),
                request.referenciaId(),
                origem.nome(),
                origem.valorUnitario(),
                request.quantidade()
        );

        return mapper.toResponse(repository.save(item));
    }

    @Transactional(readOnly = true)
    public List<ItemAtendimentoResponse> listar(Long atendimentoId) {
        if (atendimentoRepository.findByIdAndDeletadoEmIsNull(atendimentoId).isEmpty()) {
            throw new ResourceNotFoundException("Atendimento não encontrado: " + atendimentoId);
        }

        return repository.findByAtendimentoIdAndDeletadoEmIsNullOrderByCriadoEmAsc(atendimentoId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public void excluir(Long atendimentoId, Long itemId, UsuarioAutenticado usuario) {
        buscarAtendimentoEmAndamento(atendimentoId, usuario);

        ItemAtendimento item = repository.findByIdAndAtendimentoIdAndDeletadoEmIsNull(itemId, atendimentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Item do atendimento não encontrado: " + itemId));

        item.setDeletadoEm(LocalDateTime.now());
        repository.save(item);
    }

    private Atendimento buscarAtendimentoEmAndamento(Long atendimentoId, UsuarioAutenticado usuario) {
        Atendimento atendimento = atendimentoRepository.findByIdAndDeletadoEmIsNull(atendimentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Atendimento não encontrado: " + atendimentoId));

        if (atendimento.getStatus() != StatusAtendimento.EM_ANDAMENTO) {
            throw new BusinessException("Atendimento não está em andamento");
        }
        if (!usuario.isAdmin() && !Objects.equals(atendimento.getVeterinarioId(), usuario.id())) {
            throw new AccessDeniedException("Apenas o veterinário responsável pode alterar este atendimento");
        }

        return atendimento;
    }

    private OrigemItem resolverOrigem(TipoItemAtendimento tipo, Long referenciaId) {
        if (tipo == TipoItemAtendimento.CATALOGO_ITEM) {
            CatalogoItem item = catalogoItemRepository.findByIdAndDeletadoEmIsNull(referenciaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Item de catálogo não encontrado: " + referenciaId));
            validarAtivo(item.getStatus(), "Item de catálogo");
            return new OrigemItem(item.getNome(), item.getValorBase());
        }

        Medicamento medicamento = medicamentoRepository.findByIdAndDeletadoEmIsNull(referenciaId)
                .orElseThrow(() -> new ResourceNotFoundException("Medicamento não encontrado: " + referenciaId));
        validarAtivo(medicamento.getStatus(), "Medicamento");
        return new OrigemItem(medicamento.getNome(), medicamento.getValorBase());
    }

    private void validarAtivo(StatusItem status, String descricao) {
        if (status != StatusItem.ATIVO) {
            throw new BusinessException(descricao + " está inativo e não pode ser utilizado");
        }
    }

    private record OrigemItem(String nome, BigDecimal valorUnitario) {
    }
}
