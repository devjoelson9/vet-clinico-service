
package br.com.vet.clinico.application.service;

import br.com.vet.clinico.application.dto.request.RegistrarPagamentoRequest;
import br.com.vet.clinico.application.dto.response.PagamentoResponse;
import br.com.vet.clinico.application.exception.BusinessException;
import br.com.vet.clinico.application.exception.ResourceNotFoundException;
import br.com.vet.clinico.domain.enums.StatusPagamento;
import br.com.vet.clinico.domain.model.Atendimento;
import br.com.vet.clinico.domain.model.Pagamento;
import br.com.vet.clinico.domain.repository.AtendimentoRepository;
import br.com.vet.clinico.domain.repository.PagamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PagamentoService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final PagamentoRepository pagamentoRepository;
    private final AtendimentoRepository atendimentoRepository;

    @Transactional
    public PagamentoResponse registrar(
            Long atendimentoId,
            RegistrarPagamentoRequest request
    ) {
        Atendimento atendimento = buscarAtendimento(atendimentoId);

        BigDecimal valorTotal = atendimento.getValorTotal();

        if (valorTotal == null || valorTotal.compareTo(ZERO) <= 0) {
            throw new BusinessException(
                    "O atendimento não possui um valor total válido"
            );
        }

        BigDecimal pagos = somar(
                atendimentoId,
                StatusPagamento.PAGO
        );

        BigDecimal pendentes = somar(
                atendimentoId,
                StatusPagamento.PENDENTE
        );

        BigDecimal disponivel = valorTotal
                .subtract(pagos)
                .subtract(pendentes);

        if (request.valor().compareTo(disponivel) > 0) {
            throw new BusinessException(
                    "O valor informado é maior que o saldo disponível: "
                            + disponivel
            );
        }

        Pagamento pagamento = new Pagamento();
        pagamento.setAtendimentoId(atendimentoId);
        pagamento.setValor(request.valor());
        pagamento.setStatus(StatusPagamento.PENDENTE);

        Pagamento salvo = pagamentoRepository.save(pagamento);

        return toResponse(salvo);
    }

    @Transactional(readOnly = true)
    public List<PagamentoResponse> listar(Long atendimentoId) {
        buscarAtendimento(atendimentoId);

        return pagamentoRepository
                .findByAtendimentoIdOrderByRegistradoEmAsc(atendimentoId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PagamentoResponse confirmar(
            Long atendimentoId,
            Long pagamentoId
    ) {
        Atendimento atendimento = buscarAtendimento(atendimentoId);

        Pagamento pagamento = pagamentoRepository
                .findByIdAndAtendimentoId(pagamentoId, atendimentoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pagamento não encontrado: " + pagamentoId
                ));

        if (pagamento.getStatus() == StatusPagamento.PAGO) {
            return toResponse(pagamento);
        }

        BigDecimal pagos = somar(
                atendimentoId,
                StatusPagamento.PAGO
        );

        BigDecimal novoTotalPago = pagos.add(pagamento.getValor());

        if (novoTotalPago.compareTo(atendimento.getValorTotal()) > 0) {
            throw new BusinessException(
                    "A confirmação ultrapassa o valor total do atendimento"
            );
        }

        pagamento.setStatus(StatusPagamento.PAGO);
        pagamento.setPagoEm(LocalDateTime.now());

        Pagamento salvo = pagamentoRepository.save(pagamento);

        return toResponse(salvo);
    }

    private BigDecimal somar(
            Long atendimentoId,
            StatusPagamento status
    ) {
        BigDecimal total = pagamentoRepository
                .somarPorAtendimentoEStatus(atendimentoId, status);

        return total != null ? total : ZERO;
    }

    private Atendimento buscarAtendimento(Long atendimentoId) {
        return atendimentoRepository
                .findByIdAndDeletadoEmIsNull(atendimentoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Atendimento não encontrado: " + atendimentoId
                ));
    }

    private PagamentoResponse toResponse(Pagamento pagamento) {
        return new PagamentoResponse(
                pagamento.getId(),
                pagamento.getAtendimentoId(),
                pagamento.getValor(),
                pagamento.getStatus(),
                pagamento.getRegistradoEm(),
                pagamento.getPagoEm()
        );
    }
}
