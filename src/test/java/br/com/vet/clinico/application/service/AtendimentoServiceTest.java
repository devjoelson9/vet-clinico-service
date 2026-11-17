package br.com.vet.clinico.application.service;

import br.com.vet.clinico.application.dto.request.AtualizarAtendimentoRequest;
import br.com.vet.clinico.application.dto.request.IniciarAtendimentoRequest;
import br.com.vet.clinico.application.dto.response.AtendimentoResponse;
import br.com.vet.clinico.application.dto.response.HistoricoClinicoResponse;
import br.com.vet.clinico.application.event.AtendimentoEventPublisher;
import br.com.vet.clinico.application.exception.BusinessException;
import br.com.vet.clinico.application.exception.ConflictException;
import br.com.vet.clinico.application.gateway.AgendamentoGateway;
import br.com.vet.clinico.application.mapper.AtendimentoMapper;
import br.com.vet.clinico.domain.enums.StatusAtendimento;
import br.com.vet.clinico.domain.model.Atendimento;
import br.com.vet.clinico.domain.repository.AtendimentoRepository;
import br.com.vet.clinico.infrastructure.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AtendimentoServiceTest {

    @Mock
    private AtendimentoRepository repository;

    @Mock
    private AtendimentoEventPublisher eventPublisher;

    @Mock
    private ValorAtendimentoService valorAtendimentoService;

    private AtendimentoService service;

    private final UsuarioAutenticado veterinario = new UsuarioAutenticado(10L, "vet@teste.com", Set.of("VETERINARIO"));

    @BeforeEach
    void setUp() {
        service = new AtendimentoService(
                repository,
                new AtendimentoMapper(),
                eventPublisher,
                List.of(),
                valorAtendimentoService
        );
    }

    @Test
    void deveIniciarAtendimentoComVeterinarioDoToken() {
        when(repository.existsByAgendamentoIdAndDeletadoEmIsNull(1L)).thenReturn(false);
        when(repository.save(any(Atendimento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IniciarAtendimentoRequest request = new IniciarAtendimentoRequest(1L, 2L, new BigDecimal("12.50"), null);
        AtendimentoResponse response = service.iniciar(request, veterinario);

        assertThat(response.agendamentoId()).isEqualTo(1L);
        assertThat(response.animalId()).isEqualTo(2L);
        assertThat(response.veterinarioId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(StatusAtendimento.EM_ANDAMENTO);
        assertThat(response.iniciadoEm()).isNotNull();
        verify(eventPublisher).publicarIniciado(any(Atendimento.class));
    }

    @Test
    void deveAplicarServicoDoCatalogoAoIniciarAtendimento() {
        when(repository.existsByAgendamentoIdAndDeletadoEmIsNull(1L)).thenReturn(false);
        when(repository.save(any(Atendimento.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(valorAtendimentoService.calcularTotal(any(Atendimento.class))).thenReturn(new BigDecimal("100.00"));

        IniciarAtendimentoRequest request = new IniciarAtendimentoRequest(
                1L, 2L, new BigDecimal("12.50"), 5L);

        AtendimentoResponse response = service.iniciar(request, veterinario);

        verify(valorAtendimentoService).aplicarServico(any(Atendimento.class), eq(5L));
        assertThat(response.valorTotal()).isEqualByComparingTo("100.00");
    }

    @Test
    void deveValidarAgendamentoAntesDeIniciar() {
        AtomicBoolean validado = new AtomicBoolean(false);
        AgendamentoGateway gateway = (agendamentoId, animalId, veterinarioId) -> validado.set(true);
        service = new AtendimentoService(
                repository,
                new AtendimentoMapper(),
                eventPublisher,
                List.of(gateway),
                valorAtendimentoService
        );

        when(repository.existsByAgendamentoIdAndDeletadoEmIsNull(1L)).thenReturn(false);
        when(repository.save(any(Atendimento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.iniciar(new IniciarAtendimentoRequest(1L, 2L, null, null), veterinario);

        assertThat(validado).isTrue();
    }

    @Test
    void deveRecusarInicioDuplicadoParaMesmoAgendamento() {
        when(repository.existsByAgendamentoIdAndDeletadoEmIsNull(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.iniciar(new IniciarAtendimentoRequest(1L, 2L, null, null), veterinario))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void deveRecusarInicioSemIdentificacaoNoToken() {
        UsuarioAutenticado semId = new UsuarioAutenticado(null, "vet@teste.com", Set.of("VETERINARIO"));

        assertThatThrownBy(() -> service.iniciar(new IniciarAtendimentoRequest(1L, 2L, null, null), semId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deveFinalizarAtendimentoComCamposObrigatorios() {
        Atendimento atendimento = atendimentoEmAndamento();
        when(repository.findByIdAndDeletadoEmIsNull(1L)).thenReturn(Optional.of(atendimento));
        when(repository.save(any(Atendimento.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(valorAtendimentoService.calcularTotal(any(Atendimento.class)))
                .thenReturn(new BigDecimal("175.00"));

        AtendimentoResponse response = service.finalizar(1L, null, veterinario);

        assertThat(response.status()).isEqualTo(StatusAtendimento.FINALIZADO);
        assertThat(response.finalizadoEm()).isNotNull();
        assertThat(response.valorTotal()).isEqualByComparingTo("175.00");
        verify(eventPublisher).publicarFinalizado(any(Atendimento.class));
        verify(valorAtendimentoService).calcularTotal(atendimento);
    }

    @Test
    void deveRecusarFinalizacaoSemCamposObrigatorios() {
        Atendimento atendimento = atendimentoEmAndamento();
        atendimento.setDiagnostico(null);
        when(repository.findByIdAndDeletadoEmIsNull(1L)).thenReturn(Optional.of(atendimento));

        assertThatThrownBy(() -> service.finalizar(1L, null, veterinario))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Diagnóstico");
    }

    @Test
    void deveRecusarEdicaoDeAtendimentoFinalizado() {
        Atendimento atendimento = atendimentoEmAndamento();
        atendimento.setStatus(StatusAtendimento.FINALIZADO);
        when(repository.findByIdAndDeletadoEmIsNull(1L)).thenReturn(Optional.of(atendimento));

        AtualizarAtendimentoRequest request = new AtualizarAtendimentoRequest(null, "Nova queixa", null, null, null, null);

        assertThatThrownBy(() -> service.atualizar(1L, request, veterinario))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void deveRecusarAlteracaoPorOutroVeterinario() {
        Atendimento atendimento = atendimentoEmAndamento();
        when(repository.findByIdAndDeletadoEmIsNull(1L)).thenReturn(Optional.of(atendimento));

        UsuarioAutenticado outroVeterinario = new UsuarioAutenticado(99L, "outro@teste.com", Set.of("VETERINARIO"));
        AtualizarAtendimentoRequest request = new AtualizarAtendimentoRequest(null, "Nova queixa", null, null, null, null);

        assertThatThrownBy(() -> service.atualizar(1L, request, outroVeterinario))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deveCancelarAtendimentoComExclusaoLogica() {
        Atendimento atendimento = atendimentoEmAndamento();
        when(repository.findByIdAndDeletadoEmIsNull(1L)).thenReturn(Optional.of(atendimento));
        when(repository.save(any(Atendimento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.cancelar(1L, veterinario);

        assertThat(atendimento.getStatus()).isEqualTo(StatusAtendimento.CANCELADO);
        assertThat(atendimento.getDeletadoEm()).isNotNull();
        verify(eventPublisher).publicarCancelado(any(Atendimento.class));
    }

    @Test
    void deveConsultarHistoricoMapeandoDadosClinicos() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "finalizadoEm"));
        Atendimento atendimento = atendimentoFinalizado();
        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(atendimento), pageable, 1));

        Page<HistoricoClinicoResponse> pagina = service.consultarHistorico(2L, null, null, pageable);

        assertThat(pagina.getTotalElements()).isEqualTo(1);
        assertThat(pagina.getContent()).hasSize(1);
        HistoricoClinicoResponse response = pagina.getContent().get(0);
        assertThat(response.atendimentoId()).isEqualTo(1L);
        assertThat(response.agendamentoId()).isEqualTo(5L);
        assertThat(response.animalId()).isEqualTo(2L);
        assertThat(response.veterinarioId()).isEqualTo(10L);
        assertThat(response.pesoKg()).isEqualByComparingTo("12.50");
        assertThat(response.queixaSintomas()).isEqualTo("Apatia");
        assertThat(response.diagnostico()).isEqualTo("Gastrite");
        assertThat(response.tratamento()).isEqualTo("Medicação");
        assertThat(response.observacoes()).isEqualTo("Animal estável");
        assertThat(response.orientacoes()).isEqualTo("Retornar em 7 dias");
        assertThat(response.iniciadoEm()).isNotNull();
        assertThat(response.finalizadoEm()).isNotNull();
    }

    @Test
    void deveRetornarPaginaVaziaQuandoAnimalNaoPossuiHistorico() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "finalizadoEm"));
        when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(Page.empty(pageable));

        Page<HistoricoClinicoResponse> pagina = service.consultarHistorico(99L, null, null, pageable);

        assertThat(pagina).isEmpty();
        verify(repository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void deveRecusarConsultaDeHistoricoComAnimalIdInvalido() {
        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> service.consultarHistorico(null, null, null, pageable))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Animal inválido");
        assertThatThrownBy(() -> service.consultarHistorico(0L, null, null, pageable))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Animal inválido");
        assertThatThrownBy(() -> service.consultarHistorico(-1L, null, null, pageable))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Animal inválido");

        verify(repository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void deveFiltrarHistoricoPorAnimalFinalizadoENaoDeletado() {
        Pageable pageable = PageRequest.of(0, 10);
        when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(Page.empty(pageable));

        service.consultarHistorico(2L, null, null, pageable);

        Specification<Atendimento> spec = capturarEspecificacao(pageable);
        Root<Atendimento> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        when(cb.and(any(Predicate[].class))).thenReturn(mock(Predicate.class));

        spec.toPredicate(root, query, cb);

        verify(root).get("animalId");
        verify(root).get("status");
        verify(root).get("deletadoEm");
        verify(cb).equal(any(), eq(2L));
        verify(cb).equal(any(), eq(StatusAtendimento.FINALIZADO));
        verify(cb).isNull(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void deveFiltrarHistoricoPorPeriodoDeFinalizacao() {
        Pageable pageable = PageRequest.of(0, 10);
        when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(Page.empty(pageable));

        LocalDate inicio = LocalDate.of(2026, 1, 1);
        LocalDate fim = LocalDate.of(2026, 1, 31);
        service.consultarHistorico(2L, inicio, fim, pageable);

        Specification<Atendimento> spec = capturarEspecificacao(pageable);
        Root<Atendimento> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        when(cb.and(any(Predicate[].class))).thenReturn(mock(Predicate.class));
        when(cb.greaterThanOrEqualTo(any(), any(LocalDateTime.class))).thenReturn(mock(Predicate.class));
        when(cb.lessThan(any(), any(LocalDateTime.class))).thenReturn(mock(Predicate.class));

        spec.toPredicate(root, query, cb);

        verify(root, times(2)).get("finalizadoEm");
        verify(cb).greaterThanOrEqualTo(any(), eq(inicio.atStartOfDay()));
        verify(cb).lessThan(any(), eq(fim.plusDays(1).atStartOfDay()));
    }

    @SuppressWarnings("unchecked")
    private Specification<Atendimento> capturarEspecificacao(Pageable pageable) {
        ArgumentCaptor<Specification<Atendimento>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(repository).findAll(captor.capture(), eq(pageable));
        return captor.getValue();
    }

    private Atendimento atendimentoFinalizado() {
        Atendimento atendimento = atendimentoEmAndamento();
        atendimento.setStatus(StatusAtendimento.FINALIZADO);
        atendimento.setIniciadoEm(LocalDateTime.of(2026, 1, 10, 9, 0));
        atendimento.setFinalizadoEm(LocalDateTime.of(2026, 1, 10, 10, 0));
        atendimento.setPesoKg(new BigDecimal("12.50"));
        atendimento.setObservacoes("Animal estável");
        atendimento.setOrientacoes("Retornar em 7 dias");
        return atendimento;
    }

    private Atendimento atendimentoEmAndamento() {
        Atendimento atendimento = new Atendimento();
        atendimento.setId(1L);
        atendimento.setAgendamentoId(5L);
        atendimento.setAnimalId(2L);
        atendimento.setVeterinarioId(10L);
        atendimento.setStatus(StatusAtendimento.EM_ANDAMENTO);
        atendimento.setQueixaSintomas("Apatia");
        atendimento.setDiagnostico("Gastrite");
        atendimento.setTratamento("Medicação");
        return atendimento;
    }
}
