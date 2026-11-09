package br.com.vet.clinico.application.service;

import br.com.vet.clinico.application.dto.request.DefinirValorBaseRequest;
import br.com.vet.clinico.application.dto.response.ValorAtendimentoResponse;
import br.com.vet.clinico.application.exception.BusinessException;
import br.com.vet.clinico.application.exception.ResourceNotFoundException;
import br.com.vet.clinico.domain.enums.StatusAtendimento;
import br.com.vet.clinico.domain.enums.StatusItem;
import br.com.vet.clinico.domain.enums.TipoItemCatalogo;
import br.com.vet.clinico.domain.model.Atendimento;
import br.com.vet.clinico.domain.model.CatalogoItem;
import br.com.vet.clinico.domain.model.ItemAtendimento;
import br.com.vet.clinico.domain.repository.AtendimentoRepository;
import br.com.vet.clinico.domain.repository.CatalogoItemRepository;
import br.com.vet.clinico.domain.repository.ItemAtendimentoRepository;
import br.com.vet.clinico.infrastructure.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ValorAtendimentoService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final AtendimentoRepository atendimentoRepository;
    private final ItemAtendimentoRepository itemAtendimentoRepository;
    private final CatalogoItemRepository catalogoItemRepository;

    @Transactional(readOnly = true)
    public ValorAtendimentoResponse consultar(Long atendimentoId) {
        Atendimento atendimento = buscarAtendimento(atendimentoId);
        return montarResposta(atendimento, buscarItensAtivos(atendimentoId));
    }

    @Transactional
    public ValorAtendimentoResponse definirValorBase(
            Long atendimentoId,
            DefinirValorBaseRequest request,
            UsuarioAutenticado usuario
    ) {
        Atendimento atendimento = buscarAtendimentoEmAndamento(atendimentoId, usuario);
        aplicarServico(atendimento, request.servicoId());
        recalcular(atendimento);
        return montarResposta(atendimento, buscarItensAtivos(atendimentoId));
    }

    public void aplicarServico(Atendimento atendimento, Long servicoId) {
        if (servicoId == null) {
            return;
        }

        CatalogoItem servico = catalogoItemRepository.findByIdAndDeletadoEmIsNull(servicoId)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado: " + servicoId));

        if (servico.getTipo() != TipoItemCatalogo.SERVICO) {
            throw new BusinessException("Item informado não é um serviço de consulta");
        }
        if (servico.getStatus() != StatusItem.ATIVO) {
            throw new BusinessException("Serviço está inativo e não pode ser utilizado");
        }

        atendimento.setValorBase(servico.getValorBase());
    }

    public BigDecimal calcularTotal(Atendimento atendimento) {
        BigDecimal valorBase = normalizar(atendimento.getValorBase());

        if (atendimento.getId() == null) {
            return valorBase;
        }

        return valorBase.add(somarItens(buscarItensAtivos(atendimento.getId())))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public void recalcular(Atendimento atendimento) {
        atendimento.setValorTotal(calcularTotal(atendimento));
        atendimentoRepository.save(atendimento);
    }

    private ValorAtendimentoResponse montarResposta(Atendimento atendimento, List<ItemAtendimento> itens) {
        BigDecimal valorBase = normalizar(atendimento.getValorBase());
        BigDecimal valorProcedimentos = somarPorOrigem(itens, true);
        BigDecimal valorMedicamentos = somarPorOrigem(itens, false);
        BigDecimal valorTotal = valorBase.add(valorProcedimentos).add(valorMedicamentos)
                .setScale(2, RoundingMode.HALF_UP);

        return new ValorAtendimentoResponse(
                atendimento.getId(),
                valorBase,
                valorProcedimentos,
                valorMedicamentos,
                valorTotal
        );
    }

    private BigDecimal somarItens(List<ItemAtendimento> itens) {
        return itens.stream()
                .map(ItemAtendimento::getValorTotal)
                .filter(Objects::nonNull)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal somarPorOrigem(List<ItemAtendimento> itens, boolean catalogo) {
        return itens.stream()
                .filter(item -> catalogo
                        ? item.getCatalogoItemId() != null
                        : item.getMedicamentoId() != null)
                .map(ItemAtendimento::getValorTotal)
                .filter(Objects::nonNull)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizar(BigDecimal valor) {
        return valor == null ? ZERO : valor.setScale(2, RoundingMode.HALF_UP);
    }

    private List<ItemAtendimento> buscarItensAtivos(Long atendimentoId) {
        return itemAtendimentoRepository
                .findByAtendimentoIdAndDeletadoEmIsNullOrderByCriadoEmAsc(atendimentoId);
    }

    private Atendimento buscarAtendimento(Long atendimentoId) {
        return atendimentoRepository.findByIdAndDeletadoEmIsNull(atendimentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Atendimento não encontrado: " + atendimentoId));
    }

    private Atendimento buscarAtendimentoEmAndamento(Long atendimentoId, UsuarioAutenticado usuario) {
        Atendimento atendimento = buscarAtendimento(atendimentoId);

        if (atendimento.getStatus() != StatusAtendimento.EM_ANDAMENTO) {
            throw new BusinessException("Atendimento não está em andamento");
        }
        if (!usuario.isAdmin() && !Objects.equals(atendimento.getVeterinarioId(), usuario.id())) {
            throw new AccessDeniedException("Apenas o veterinário responsável pode alterar este atendimento");
        }

        return atendimento;
    }
}
