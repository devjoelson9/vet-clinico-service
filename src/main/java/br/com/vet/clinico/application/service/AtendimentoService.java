package br.com.vet.clinico.application.service;

import br.com.vet.clinico.application.dto.request.AtualizarAtendimentoRequest;
import br.com.vet.clinico.application.dto.request.IniciarAtendimentoRequest;
import br.com.vet.clinico.application.dto.response.AtendimentoResponse;
import br.com.vet.clinico.application.dto.response.HistoricoClinicoResponse;
import br.com.vet.clinico.application.event.AtendimentoEventPublisher;
import br.com.vet.clinico.application.exception.BusinessException;
import br.com.vet.clinico.application.exception.ConflictException;
import br.com.vet.clinico.application.exception.ResourceNotFoundException;
import br.com.vet.clinico.application.gateway.AgendamentoGateway;
import br.com.vet.clinico.application.mapper.AtendimentoMapper;
import br.com.vet.clinico.domain.enums.StatusAtendimento;
import br.com.vet.clinico.domain.model.Atendimento;
import br.com.vet.clinico.domain.repository.AtendimentoRepository;
import br.com.vet.clinico.infrastructure.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AtendimentoService {

    private final AtendimentoRepository repository;
    private final AtendimentoMapper mapper;
    private final AtendimentoEventPublisher eventPublisher;
    private final List<AgendamentoGateway> agendamentoGateways;
    private final ValorAtendimentoService valorAtendimentoService;

    @Transactional
    public AtendimentoResponse iniciar(IniciarAtendimentoRequest request, UsuarioAutenticado usuario) {
        Long veterinarioId = usuario.id();
        if (veterinarioId == null) {
            throw new AccessDeniedException("Token sem identificação do usuário");
        }

        if (repository.existsByAgendamentoIdAndDeletadoEmIsNull(request.agendamentoId())) {
            throw new ConflictException("Já existe um atendimento ativo para o agendamento informado");
        }

        agendamentoGateways.forEach(gateway ->
                gateway.validarInicioAtendimento(request.agendamentoId(), request.animalId(), veterinarioId));

        Atendimento atendimento = mapper.toEntity(request, veterinarioId);
        valorAtendimentoService.aplicarServico(atendimento, request.servicoId());
        atendimento.setValorTotal(valorAtendimentoService.calcularTotal(atendimento));

        Atendimento salvo = repository.save(atendimento);
        eventPublisher.publicarIniciado(salvo);
        return mapper.toResponse(salvo);
    }

    @Transactional(readOnly = true)
    public Page<AtendimentoResponse> listar(
            Long animalId,
            Long veterinarioId,
            StatusAtendimento status,
            LocalDate inicio,
            LocalDate fim,
            Pageable pageable
    ) {
        Specification<Atendimento> spec = (root, query, cb) -> cb.isNull(root.get("deletadoEm"));

        if (animalId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("animalId"), animalId));
        }
        if (veterinarioId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("veterinarioId"), veterinarioId));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (inicio != null) {
            LocalDateTime inicioDateTime = inicio.atStartOfDay();
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("iniciadoEm"), inicioDateTime));
        }
        if (fim != null) {
            LocalDateTime fimDateTime = fim.plusDays(1).atStartOfDay();
            spec = spec.and((root, query, cb) ->
                    cb.lessThan(root.get("iniciadoEm"), fimDateTime));
        }

        return repository.findAll(spec, pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public AtendimentoResponse buscarPorId(Long id) {
        return mapper.toResponse(buscarAtendimento(id));
    }

    @Transactional(readOnly = true)
    public Page<HistoricoClinicoResponse> consultarHistorico(
            Long animalId,
            LocalDate inicio,
            LocalDate fim,
            Pageable pageable
    ) {
        if (animalId == null || animalId <= 0) {
            throw new BusinessException("Animal inválido para consulta de histórico");
        }

        Specification<Atendimento> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("animalId"), animalId),
                cb.equal(root.get("status"), StatusAtendimento.FINALIZADO),
                cb.isNull(root.get("deletadoEm"))
        );

        if (inicio != null) {
            LocalDateTime inicioDateTime = inicio.atStartOfDay();
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("finalizadoEm"), inicioDateTime));
        }
        if (fim != null) {
            LocalDateTime fimDateTime = fim.plusDays(1).atStartOfDay();
            spec = spec.and((root, query, cb) ->
                    cb.lessThan(root.get("finalizadoEm"), fimDateTime));
        }

        return repository.findAll(spec, pageable).map(mapper::toHistoricoResponse);
    }

    @Transactional
    public AtendimentoResponse atualizar(Long id, AtualizarAtendimentoRequest request, UsuarioAutenticado usuario) {
        Atendimento atendimento = buscarAtendimento(id);
        validarEmAndamento(atendimento);
        validarResponsavel(atendimento, usuario);

        mapper.updateEntity(atendimento, request);
        return mapper.toResponse(repository.save(atendimento));
    }

    @Transactional
    public AtendimentoResponse finalizar(Long id, AtualizarAtendimentoRequest request, UsuarioAutenticado usuario) {
        Atendimento atendimento = buscarAtendimento(id);
        validarEmAndamento(atendimento);
        validarResponsavel(atendimento, usuario);

        if (request != null) {
            mapper.updateEntity(atendimento, request);
        }
        validarCamposObrigatorios(atendimento);

        atendimento.setStatus(StatusAtendimento.FINALIZADO);
        atendimento.setFinalizadoEm(LocalDateTime.now());
        atendimento.setValorTotal(valorAtendimentoService.calcularTotal(atendimento));

        Atendimento salvo = repository.save(atendimento);
        eventPublisher.publicarFinalizado(salvo);
        return mapper.toResponse(salvo);
    }

    @Transactional
    public void cancelar(Long id, UsuarioAutenticado usuario) {
        Atendimento atendimento = buscarAtendimento(id);
        validarEmAndamento(atendimento);
        validarResponsavel(atendimento, usuario);

        atendimento.setStatus(StatusAtendimento.CANCELADO);
        atendimento.setDeletadoEm(LocalDateTime.now());

        Atendimento salvo = repository.save(atendimento);
        eventPublisher.publicarCancelado(salvo);
    }

    private Atendimento buscarAtendimento(Long id) {
        return repository.findByIdAndDeletadoEmIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Atendimento não encontrado: " + id));
    }

    private void validarEmAndamento(Atendimento atendimento) {
        if (atendimento.getStatus() != StatusAtendimento.EM_ANDAMENTO) {
            throw new BusinessException("Atendimento não está em andamento");
        }
    }

    private void validarResponsavel(Atendimento atendimento, UsuarioAutenticado usuario) {
        if (!usuario.isAdmin() && !Objects.equals(atendimento.getVeterinarioId(), usuario.id())) {
            throw new AccessDeniedException("Apenas o veterinário responsável pode alterar este atendimento");
        }
    }

    private void validarCamposObrigatorios(Atendimento atendimento) {
        if (atendimento.getQueixaSintomas() == null || atendimento.getQueixaSintomas().isBlank()) {
            throw new BusinessException("Queixa ou sintomas é obrigatório para finalizar o atendimento");
        }
        if (atendimento.getDiagnostico() == null || atendimento.getDiagnostico().isBlank()) {
            throw new BusinessException("Diagnóstico é obrigatório para finalizar o atendimento");
        }
        if (atendimento.getTratamento() == null || atendimento.getTratamento().isBlank()) {
            throw new BusinessException("Tratamento é obrigatório para finalizar o atendimento");
        }
    }
}
