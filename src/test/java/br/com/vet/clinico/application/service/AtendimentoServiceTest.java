package br.com.vet.clinico.application.service;

import br.com.vet.clinico.application.dto.request.AtualizarAtendimentoRequest;
import br.com.vet.clinico.application.dto.request.IniciarAtendimentoRequest;
import br.com.vet.clinico.application.dto.response.AtendimentoResponse;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AtendimentoServiceTest {

    @Mock
    private AtendimentoRepository repository;

    @Mock
    private AtendimentoEventPublisher eventPublisher;

    private AtendimentoService service;

    private final UsuarioAutenticado veterinario = new UsuarioAutenticado(10L, "vet@teste.com", Set.of("VETERINARIO"));

    @BeforeEach
    void setUp() {
        service = new AtendimentoService(repository, new AtendimentoMapper(), eventPublisher, List.of());
    }

    @Test
    void deveIniciarAtendimentoComVeterinarioDoToken() {
        when(repository.existsByAgendamentoIdAndDeletadoEmIsNull(1L)).thenReturn(false);
        when(repository.save(any(Atendimento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IniciarAtendimentoRequest request = new IniciarAtendimentoRequest(1L, 2L, new BigDecimal("12.50"));
        AtendimentoResponse response = service.iniciar(request, veterinario);

        assertThat(response.agendamentoId()).isEqualTo(1L);
        assertThat(response.animalId()).isEqualTo(2L);
        assertThat(response.veterinarioId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(StatusAtendimento.EM_ANDAMENTO);
        assertThat(response.iniciadoEm()).isNotNull();
        verify(eventPublisher).publicarIniciado(any(Atendimento.class));
    }

    @Test
    void deveValidarAgendamentoAntesDeIniciar() {
        AtomicBoolean validado = new AtomicBoolean(false);
        AgendamentoGateway gateway = (agendamentoId, animalId, veterinarioId) -> validado.set(true);
        service = new AtendimentoService(repository, new AtendimentoMapper(), eventPublisher, List.of(gateway));

        when(repository.existsByAgendamentoIdAndDeletadoEmIsNull(1L)).thenReturn(false);
        when(repository.save(any(Atendimento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.iniciar(new IniciarAtendimentoRequest(1L, 2L, null), veterinario);

        assertThat(validado).isTrue();
    }

    @Test
    void deveRecusarInicioDuplicadoParaMesmoAgendamento() {
        when(repository.existsByAgendamentoIdAndDeletadoEmIsNull(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.iniciar(new IniciarAtendimentoRequest(1L, 2L, null), veterinario))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void deveRecusarInicioSemIdentificacaoNoToken() {
        UsuarioAutenticado semId = new UsuarioAutenticado(null, "vet@teste.com", Set.of("VETERINARIO"));

        assertThatThrownBy(() -> service.iniciar(new IniciarAtendimentoRequest(1L, 2L, null), semId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deveFinalizarAtendimentoComCamposObrigatorios() {
        Atendimento atendimento = atendimentoEmAndamento();
        when(repository.findByIdAndDeletadoEmIsNull(1L)).thenReturn(Optional.of(atendimento));
        when(repository.save(any(Atendimento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AtendimentoResponse response = service.finalizar(1L, null, veterinario);

        assertThat(response.status()).isEqualTo(StatusAtendimento.FINALIZADO);
        assertThat(response.finalizadoEm()).isNotNull();
        verify(eventPublisher).publicarFinalizado(any(Atendimento.class));
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
