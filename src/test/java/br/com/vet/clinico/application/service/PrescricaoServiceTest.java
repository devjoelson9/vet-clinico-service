package br.com.vet.clinico.application.service;

import br.com.vet.clinico.application.dto.request.AtualizarPrescricaoRequest;
import br.com.vet.clinico.application.dto.request.ItemPrescricaoRequest;
import br.com.vet.clinico.application.dto.request.RegistrarPrescricaoRequest;
import br.com.vet.clinico.application.dto.response.ItemPrescricaoResponse;
import br.com.vet.clinico.application.dto.response.PrescricaoResponse;
import br.com.vet.clinico.application.exception.BusinessException;
import br.com.vet.clinico.application.exception.ConflictException;
import br.com.vet.clinico.application.exception.ResourceNotFoundException;
import br.com.vet.clinico.application.mapper.PrescricaoMapper;
import br.com.vet.clinico.domain.enums.StatusAtendimento;
import br.com.vet.clinico.domain.enums.StatusItem;
import br.com.vet.clinico.domain.model.Atendimento;
import br.com.vet.clinico.domain.model.ItemPrescricao;
import br.com.vet.clinico.domain.model.Medicamento;
import br.com.vet.clinico.domain.model.Prescricao;
import br.com.vet.clinico.domain.repository.AtendimentoRepository;
import br.com.vet.clinico.domain.repository.ItemPrescricaoRepository;
import br.com.vet.clinico.domain.repository.MedicamentoRepository;
import br.com.vet.clinico.domain.repository.PrescricaoRepository;
import br.com.vet.clinico.infrastructure.pdf.PrescricaoPdfGenerator;
import br.com.vet.clinico.infrastructure.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrescricaoServiceTest {

    @Mock
    private PrescricaoRepository prescricaoRepository;

    @Mock
    private ItemPrescricaoRepository itemPrescricaoRepository;

    @Mock
    private AtendimentoRepository atendimentoRepository;

    @Mock
    private MedicamentoRepository medicamentoRepository;

    @Mock
    private PrescricaoPdfGenerator pdfGenerator;

    private PrescricaoService service;

    private final UsuarioAutenticado veterinario =
            new UsuarioAutenticado(10L, "vet@teste.com", Set.of("VETERINARIO"));

    @BeforeEach
    void setUp() {
        service = new PrescricaoService(
                prescricaoRepository,
                itemPrescricaoRepository,
                atendimentoRepository,
                medicamentoRepository,
                new PrescricaoMapper(),
                pdfGenerator
        );
    }

    @Test
    void deveRegistrarPrescricaoComItensESnapshotDoMedicamento() {
        when(atendimentoRepository.findByIdAndDeletadoEmIsNull(1L))
                .thenReturn(Optional.of(atendimentoEmAndamento()));
        when(prescricaoRepository.existsByAtendimentoIdAndDeletadoEmIsNull(1L)).thenReturn(false);
        when(prescricaoRepository.save(any(Prescricao.class))).thenAnswer(invocation -> {
            Prescricao prescricao = invocation.getArgument(0);
            prescricao.setId(50L);
            return prescricao;
        });
        when(medicamentoRepository.findByIdAndDeletadoEmIsNull(7L))
                .thenReturn(Optional.of(medicamentoAtivo(7L, "Dipirona")));
        when(medicamentoRepository.findByIdAndDeletadoEmIsNull(8L))
                .thenReturn(Optional.of(medicamentoAtivo(8L, "Prednisona")));
        when(itemPrescricaoRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PrescricaoResponse response = service.registrar(1L, requestComDoisItens(), veterinario);

        assertThat(response.id()).isEqualTo(50L);
        assertThat(response.atendimentoId()).isEqualTo(1L);
        assertThat(response.dataEmissao()).isEqualTo(LocalDate.of(2026, 11, 10));
        assertThat(response.orientacoesGerais()).isEqualTo("Administrar após a alimentação");
        assertThat(response.itens()).hasSize(2);
        assertThat(response.itens().get(0).nomeMedicamento()).isEqualTo("Dipirona");
        assertThat(response.itens().get(0).prescricaoId()).isEqualTo(50L);
        assertThat(response.itens().get(0).dosagemConcentracao()).isEqualTo("500 mg");
        assertThat(response.itens().get(1).nomeMedicamento()).isEqualTo("Prednisona");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ItemPrescricao>> captor = ArgumentCaptor.forClass(List.class);
        verify(itemPrescricaoRepository).saveAll(captor.capture());
        assertThat(captor.getValue())
                .extracting(ItemPrescricao::getMedicamentoId)
                .containsExactly(7L, 8L);
    }

    @Test
    void deveRecusarSegundaPrescricaoAtivaNoMesmoAtendimento() {
        when(atendimentoRepository.findByIdAndDeletadoEmIsNull(1L))
                .thenReturn(Optional.of(atendimentoEmAndamento()));
        when(prescricaoRepository.existsByAtendimentoIdAndDeletadoEmIsNull(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(1L, requestComDoisItens(), veterinario))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("já possui prescrição ativa");

        verify(prescricaoRepository, never()).save(any(Prescricao.class));
        verifyNoInteractions(medicamentoRepository, itemPrescricaoRepository);
    }

    @Test
    void deveRecusarPrescricaoEmAtendimentoFinalizado() {
        Atendimento atendimento = atendimentoEmAndamento();
        atendimento.setStatus(StatusAtendimento.FINALIZADO);
        when(atendimentoRepository.findByIdAndDeletadoEmIsNull(1L)).thenReturn(Optional.of(atendimento));

        assertThatThrownBy(() -> service.registrar(1L, requestComDoisItens(), veterinario))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não está em andamento");

        verifyNoInteractions(prescricaoRepository, medicamentoRepository, itemPrescricaoRepository);
    }

    @Test
    void deveRecusarPrescricaoPorOutroVeterinario() {
        when(atendimentoRepository.findByIdAndDeletadoEmIsNull(1L))
                .thenReturn(Optional.of(atendimentoEmAndamento()));
        UsuarioAutenticado outro = new UsuarioAutenticado(99L, "outro@teste.com", Set.of("VETERINARIO"));

        assertThatThrownBy(() -> service.registrar(1L, requestComDoisItens(), outro))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(prescricaoRepository, medicamentoRepository, itemPrescricaoRepository);
    }

    @Test
    void devePermitirPrescricaoPorAdmin() {
        when(atendimentoRepository.findByIdAndDeletadoEmIsNull(1L))
                .thenReturn(Optional.of(atendimentoEmAndamento()));
        when(prescricaoRepository.existsByAtendimentoIdAndDeletadoEmIsNull(1L)).thenReturn(false);
        when(prescricaoRepository.save(any(Prescricao.class))).thenAnswer(invocation -> {
            Prescricao prescricao = invocation.getArgument(0);
            prescricao.setId(50L);
            return prescricao;
        });
        when(medicamentoRepository.findByIdAndDeletadoEmIsNull(7L))
                .thenReturn(Optional.of(medicamentoAtivo(7L, "Dipirona")));
        when(medicamentoRepository.findByIdAndDeletadoEmIsNull(8L))
                .thenReturn(Optional.of(medicamentoAtivo(8L, "Prednisona")));
        when(itemPrescricaoRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        UsuarioAutenticado admin = new UsuarioAutenticado(1L, "admin@teste.com", Set.of("ADMIN"));

        PrescricaoResponse response = service.registrar(1L, requestComDoisItens(), admin);

        assertThat(response.itens()).hasSize(2);
    }

    @Test
    void deveRecusarMedicamentoInexistente() {
        when(atendimentoRepository.findByIdAndDeletadoEmIsNull(1L))
                .thenReturn(Optional.of(atendimentoEmAndamento()));
        when(prescricaoRepository.existsByAtendimentoIdAndDeletadoEmIsNull(1L)).thenReturn(false);
        when(prescricaoRepository.save(any(Prescricao.class))).thenAnswer(invocation -> {
            Prescricao prescricao = invocation.getArgument(0);
            prescricao.setId(50L);
            return prescricao;
        });
        when(medicamentoRepository.findByIdAndDeletadoEmIsNull(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(1L, requestComDoisItens(), veterinario))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Medicamento não encontrado");

        verify(itemPrescricaoRepository, never()).saveAll(anyList());
    }

    @Test
    void deveRecusarMedicamentoInativo() {
        when(atendimentoRepository.findByIdAndDeletadoEmIsNull(1L))
                .thenReturn(Optional.of(atendimentoEmAndamento()));
        when(prescricaoRepository.existsByAtendimentoIdAndDeletadoEmIsNull(1L)).thenReturn(false);
        when(prescricaoRepository.save(any(Prescricao.class))).thenAnswer(invocation -> {
            Prescricao prescricao = invocation.getArgument(0);
            prescricao.setId(50L);
            return prescricao;
        });
        Medicamento medicamento = medicamentoAtivo(7L, "Dipirona");
        medicamento.setStatus(StatusItem.INATIVO);
        when(medicamentoRepository.findByIdAndDeletadoEmIsNull(7L)).thenReturn(Optional.of(medicamento));

        assertThatThrownBy(() -> service.registrar(1L, requestComDoisItens(), veterinario))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inativo");
    }

    @Test
    void deveConsultarPrescricaoAtivaComItens() {
        when(atendimentoRepository.findByIdAndDeletadoEmIsNull(1L))
                .thenReturn(Optional.of(atendimentoEmAndamento()));
        when(prescricaoRepository.findByAtendimentoIdAndDeletadoEmIsNull(1L))
                .thenReturn(Optional.of(prescricaoPersistida()));
        when(itemPrescricaoRepository.findByPrescricaoIdAndDeletadoEmIsNullOrderByCriadoEmAsc(50L))
                .thenReturn(List.of(itemPersistido(9L, 7L, "Dipirona")));

        PrescricaoResponse response = service.consultar(1L);

        assertThat(response.id()).isEqualTo(50L);
        assertThat(response.itens()).hasSize(1);
        assertThat(response.itens().get(0).nomeMedicamento()).isEqualTo("Dipirona");
    }

    @Test
    void deveRecusarConsultaDePrescricaoInexistente() {
        when(atendimentoRepository.findByIdAndDeletadoEmIsNull(1L))
                .thenReturn(Optional.of(atendimentoEmAndamento()));
        when(prescricaoRepository.findByAtendimentoIdAndDeletadoEmIsNull(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consultar(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Prescrição não encontrada");
    }

    @Test
    void deveAtualizarDataEOrientacoesDaPrescricao() {
        Prescricao prescricao = prescricaoPersistida();
        when(atendimentoRepository.findByIdAndDeletadoEmIsNull(1L))
                .thenReturn(Optional.of(atendimentoEmAndamento()));
        when(prescricaoRepository.findByAtendimentoIdAndDeletadoEmIsNull(1L))
                .thenReturn(Optional.of(prescricao));
        when(prescricaoRepository.save(any(Prescricao.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(itemPrescricaoRepository.findByPrescricaoIdAndDeletadoEmIsNullOrderByCriadoEmAsc(50L))
                .thenReturn(List.of());

        PrescricaoResponse response = service.atualizar(
                1L,
                new AtualizarPrescricaoRequest(LocalDate.of(2026, 11, 11), "Nova orientação"),
                veterinario
        );

        assertThat(response.dataEmissao()).isEqualTo(LocalDate.of(2026, 11, 11));
        assertThat(response.orientacoesGerais()).isEqualTo("Nova orientação");
        verify(prescricaoRepository).save(prescricao);
    }

    @Test
    void deveAdicionarItemNaPrescricaoAtiva() {
        when(atendimentoRepository.findByIdAndDeletadoEmIsNull(1L))
                .thenReturn(Optional.of(atendimentoEmAndamento()));
        when(prescricaoRepository.findByAtendimentoIdAndDeletadoEmIsNull(1L))
                .thenReturn(Optional.of(prescricaoPersistida()));
        when(medicamentoRepository.findByIdAndDeletadoEmIsNull(8L))
                .thenReturn(Optional.of(medicamentoAtivo(8L, "Prednisona")));
        when(itemPrescricaoRepository.save(any(ItemPrescricao.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ItemPrescricaoResponse response = service.adicionarItem(
                1L, itemRequest(8L), veterinario);

        assertThat(response.prescricaoId()).isEqualTo(50L);
        assertThat(response.nomeMedicamento()).isEqualTo("Prednisona");
        assertThat(response.posologia()).isEqualTo("1 comprimido a cada 8 horas");
    }

    @Test
    void deveExcluirItemLogicamente() {
        ItemPrescricao item = itemPersistido(9L, 7L, "Dipirona");
        when(atendimentoRepository.findByIdAndDeletadoEmIsNull(1L))
                .thenReturn(Optional.of(atendimentoEmAndamento()));
        when(prescricaoRepository.findByAtendimentoIdAndDeletadoEmIsNull(1L))
                .thenReturn(Optional.of(prescricaoPersistida()));
        when(itemPrescricaoRepository.findByIdAndPrescricaoIdAndDeletadoEmIsNull(9L, 50L))
                .thenReturn(Optional.of(item));
        when(itemPrescricaoRepository.save(any(ItemPrescricao.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.excluirItem(1L, 9L, veterinario);

        assertThat(item.getDeletadoEm()).isNotNull();
        verify(itemPrescricaoRepository).save(item);
    }

    @Test
    void deveExcluirPrescricaoEItensLogicamente() {
        Prescricao prescricao = prescricaoPersistida();
        ItemPrescricao primeiro = itemPersistido(9L, 7L, "Dipirona");
        ItemPrescricao segundo = itemPersistido(10L, 8L, "Prednisona");
        when(atendimentoRepository.findByIdAndDeletadoEmIsNull(1L))
                .thenReturn(Optional.of(atendimentoEmAndamento()));
        when(prescricaoRepository.findByAtendimentoIdAndDeletadoEmIsNull(1L))
                .thenReturn(Optional.of(prescricao));
        when(itemPrescricaoRepository.findByPrescricaoIdAndDeletadoEmIsNullOrderByCriadoEmAsc(50L))
                .thenReturn(List.of(primeiro, segundo));
        when(itemPrescricaoRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(prescricaoRepository.save(any(Prescricao.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.excluir(1L, veterinario);

        assertThat(prescricao.getDeletadoEm()).isNotNull();
        assertThat(primeiro.getDeletadoEm()).isNotNull();
        assertThat(segundo.getDeletadoEm()).isNotNull();
        verify(prescricaoRepository).save(prescricao);
    }

    @Test
    void deveGerarPdfDaPrescricao() {
        byte[] pdf = "%PDF-1.7".getBytes();
        Atendimento atendimento = atendimentoEmAndamento();
        Prescricao prescricao = prescricaoPersistida();
        List<ItemPrescricao> itens = List.of(itemPersistido(9L, 7L, "Dipirona"));
        when(atendimentoRepository.findByIdAndDeletadoEmIsNull(1L)).thenReturn(Optional.of(atendimento));
        when(prescricaoRepository.findByAtendimentoIdAndDeletadoEmIsNull(1L))
                .thenReturn(Optional.of(prescricao));
        when(itemPrescricaoRepository.findByPrescricaoIdAndDeletadoEmIsNullOrderByCriadoEmAsc(50L))
                .thenReturn(itens);
        when(pdfGenerator.gerar(atendimento, prescricao, itens)).thenReturn(pdf);

        byte[] resultado = service.gerarPdf(1L);

        assertThat(resultado).isEqualTo(pdf);
        verify(pdfGenerator).gerar(atendimento, prescricao, itens);
    }

    @Test
    void deveRecusarPdfDeAtendimentoInexistente() {
        when(atendimentoRepository.findByIdAndDeletadoEmIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.gerarPdf(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(prescricaoRepository, itemPrescricaoRepository, pdfGenerator);
    }

    private RegistrarPrescricaoRequest requestComDoisItens() {
        return new RegistrarPrescricaoRequest(
                LocalDate.of(2026, 11, 10),
                "Administrar após a alimentação",
                List.of(itemRequest(7L), itemRequest(8L))
        );
    }

    private ItemPrescricaoRequest itemRequest(Long medicamentoId) {
        return new ItemPrescricaoRequest(
                medicamentoId,
                "500 mg",
                "1 comprimido a cada 8 horas",
                "5 dias",
                "Oral"
        );
    }

    private Atendimento atendimentoEmAndamento() {
        Atendimento atendimento = new Atendimento();
        atendimento.setId(1L);
        atendimento.setAnimalId(2L);
        atendimento.setVeterinarioId(10L);
        atendimento.setStatus(StatusAtendimento.EM_ANDAMENTO);
        return atendimento;
    }

    private Medicamento medicamentoAtivo(Long id, String nome) {
        Medicamento medicamento = new Medicamento();
        medicamento.setId(id);
        medicamento.setNome(nome);
        medicamento.setStatus(StatusItem.ATIVO);
        return medicamento;
    }

    private Prescricao prescricaoPersistida() {
        Prescricao prescricao = new Prescricao();
        prescricao.setId(50L);
        prescricao.setAtendimentoId(1L);
        prescricao.setDataEmissao(LocalDate.of(2026, 11, 10));
        prescricao.setOrientacoesGerais("Administrar após a alimentação");
        return prescricao;
    }

    private ItemPrescricao itemPersistido(Long id, Long medicamentoId, String nome) {
        ItemPrescricao item = new ItemPrescricao();
        item.setId(id);
        item.setPrescricaoId(50L);
        item.setMedicamentoId(medicamentoId);
        item.setNomeMedicamento(nome);
        item.setDosagemConcentracao("500 mg");
        item.setPosologia("1 comprimido a cada 8 horas");
        item.setDuracaoTratamento("5 dias");
        item.setViaAdministracao("Oral");
        return item;
    }
}
