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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValorAtendimentoServiceTest {

    @Mock
    private AtendimentoRepository atendimentoRepository;

    @Mock
    private ItemAtendimentoRepository itemAtendimentoRepository;

    @Mock
    private CatalogoItemRepository catalogoItemRepository;

    private ValorAtendimentoService service;

    private final UsuarioAutenticado veterinario =
            new UsuarioAutenticado(10L, "vet@teste.com", Set.of("VETERINARIO"));

    @BeforeEach
    void setUp() {
        service = new ValorAtendimentoService(
                atendimentoRepository,
                itemAtendimentoRepository,
                catalogoItemRepository
        );
    }

    @Test
    void deveConsultarValorComBaseProcedimentosEMedicamentos() {
        Atendimento atendimento = atendimentoEmAndamento();
        atendimento.setValorBase(new BigDecimal("100.00"));
        when(atendimentoRepository.findByIdAndDeletadoEmIsNull(1L)).thenReturn(Optional.of(atendimento));
        when(itemAtendimentoRepository.findByAtendimentoIdAndDeletadoEmIsNullOrderByCriadoEmAsc(1L))
                .thenReturn(List.of(
                        itemCatalogo(new BigDecimal("50.00")),
                        itemMedicamento(new BigDecimal("25.00"))
                ));

        ValorAtendimentoResponse response = service.consultar(1L);

        assertThat(response.atendimentoId()).isEqualTo(1L);
        assertThat(response.valorBase()).isEqualByComparingTo("100.00");
        assertThat(response.valorProcedimentos()).isEqualByComparingTo("50.00");
        assertThat(response.valorMedicamentos()).isEqualByComparingTo("25.00");
        assertThat(response.valorTotal()).isEqualByComparingTo("175.00");
    }

    @Test
    void deveTratarValorBaseNuloComoZero() {
        Atendimento atendimento = atendimentoEmAndamento();
        when(atendimentoRepository.findByIdAndDeletadoEmIsNull(1L)).thenReturn(Optional.of(atendimento));
        when(itemAtendimentoRepository.findByAtendimentoIdAndDeletadoEmIsNullOrderByCriadoEmAsc(1L))
                .thenReturn(List.of(itemCatalogo(new BigDecimal("30.00"))));

        ValorAtendimentoResponse response = service.consultar(1L);

        assertThat(response.valorBase()).isEqualByComparingTo("0.00");
        assertThat(response.valorTotal()).isEqualByComparingTo("30.00");
    }

    @Test
    void deveRecalcularValorTotalDoAtendimento() {
        Atendimento atendimento = atendimentoEmAndamento();
        atendimento.setValorBase(new BigDecimal("100.00"));
        when(itemAtendimentoRepository.findByAtendimentoIdAndDeletadoEmIsNullOrderByCriadoEmAsc(1L))
                .thenReturn(List.of(
                        itemCatalogo(new BigDecimal("50.00")),
                        itemMedicamento(new BigDecimal("25.00"))
                ));
        when(atendimentoRepository.save(any(Atendimento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.recalcular(atendimento);

        assertThat(atendimento.getValorTotal()).isEqualByComparingTo("175.00");
        verify(atendimentoRepository).save(atendimento);
    }

    @Test
    void deveCalcularTotalDeAtendimentoNovoSemConsultarItens() {
        Atendimento atendimento = new Atendimento();
        atendimento.setValorBase(new BigDecimal("100.00"));

        BigDecimal total = service.calcularTotal(atendimento);

        assertThat(total).isEqualByComparingTo("100.00");
        verifyNoInteractions(itemAtendimentoRepository, atendimentoRepository);
    }

    @Test
    void deveDefinirValorBaseComServicoDoCatalogo() {
        Atendimento atendimento = atendimentoEmAndamento();
        when(atendimentoRepository.findByIdAndDeletadoEmIsNull(1L)).thenReturn(Optional.of(atendimento));
        when(catalogoItemRepository.findByIdAndDeletadoEmIsNull(5L))
                .thenReturn(Optional.of(servicoAtivo(new BigDecimal("120.00"))));
        when(itemAtendimentoRepository.findByAtendimentoIdAndDeletadoEmIsNullOrderByCriadoEmAsc(1L))
                .thenReturn(List.of());
        when(atendimentoRepository.save(any(Atendimento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ValorAtendimentoResponse response = service.definirValorBase(
                1L, new DefinirValorBaseRequest(5L), veterinario);

        assertThat(atendimento.getValorBase()).isEqualByComparingTo("120.00");
        assertThat(atendimento.getValorTotal()).isEqualByComparingTo("120.00");
        assertThat(response.valorBase()).isEqualByComparingTo("120.00");
        assertThat(response.valorTotal()).isEqualByComparingTo("120.00");
        verify(atendimentoRepository).save(atendimento);
    }

    @Test
    void deveRecusarServicoQueNaoSejaDoTipoServico() {
        Atendimento atendimento = atendimentoEmAndamento();
        CatalogoItem procedimento = servicoAtivo(new BigDecimal("50.00"));
        procedimento.setTipo(TipoItemCatalogo.PROCEDIMENTO);
        when(atendimentoRepository.findByIdAndDeletadoEmIsNull(1L)).thenReturn(Optional.of(atendimento));
        when(catalogoItemRepository.findByIdAndDeletadoEmIsNull(5L)).thenReturn(Optional.of(procedimento));

        assertThatThrownBy(() -> service.definirValorBase(1L, new DefinirValorBaseRequest(5L), veterinario))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não é um serviço");

        verify(atendimentoRepository, never()).save(any(Atendimento.class));
    }

    @Test
    void deveRecusarServicoInativo() {
        Atendimento atendimento = atendimentoEmAndamento();
        CatalogoItem servico = servicoAtivo(new BigDecimal("100.00"));
        servico.setStatus(StatusItem.INATIVO);
        when(atendimentoRepository.findByIdAndDeletadoEmIsNull(1L)).thenReturn(Optional.of(atendimento));
        when(catalogoItemRepository.findByIdAndDeletadoEmIsNull(5L)).thenReturn(Optional.of(servico));

        assertThatThrownBy(() -> service.definirValorBase(1L, new DefinirValorBaseRequest(5L), veterinario))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inativo");
    }

    @Test
    void deveRecusarServicoInexistente() {
        Atendimento atendimento = atendimentoEmAndamento();
        when(atendimentoRepository.findByIdAndDeletadoEmIsNull(1L)).thenReturn(Optional.of(atendimento));
        when(catalogoItemRepository.findByIdAndDeletadoEmIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.definirValorBase(1L, new DefinirValorBaseRequest(99L), veterinario))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Serviço não encontrado");
    }

    @Test
    void deveRecusarDefinicaoEmAtendimentoFinalizado() {
        Atendimento atendimento = atendimentoEmAndamento();
        atendimento.setStatus(StatusAtendimento.FINALIZADO);
        when(atendimentoRepository.findByIdAndDeletadoEmIsNull(1L)).thenReturn(Optional.of(atendimento));

        assertThatThrownBy(() -> service.definirValorBase(1L, new DefinirValorBaseRequest(5L), veterinario))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não está em andamento");

        verifyNoInteractions(catalogoItemRepository, itemAtendimentoRepository);
    }

    @Test
    void deveRecusarDefinicaoPorOutroVeterinario() {
        when(atendimentoRepository.findByIdAndDeletadoEmIsNull(1L))
                .thenReturn(Optional.of(atendimentoEmAndamento()));
        UsuarioAutenticado outro = new UsuarioAutenticado(99L, "outro@teste.com", Set.of("VETERINARIO"));

        assertThatThrownBy(() -> service.definirValorBase(1L, new DefinirValorBaseRequest(5L), outro))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(catalogoItemRepository, itemAtendimentoRepository);
    }

    @Test
    void deveRecusarConsultaDeAtendimentoInexistente() {
        when(atendimentoRepository.findByIdAndDeletadoEmIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consultar(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(itemAtendimentoRepository);
    }

    private Atendimento atendimentoEmAndamento() {
        Atendimento atendimento = new Atendimento();
        atendimento.setId(1L);
        atendimento.setVeterinarioId(10L);
        atendimento.setStatus(StatusAtendimento.EM_ANDAMENTO);
        return atendimento;
    }

    private CatalogoItem servicoAtivo(BigDecimal valorBase) {
        CatalogoItem servico = new CatalogoItem();
        servico.setId(5L);
        servico.setTipo(TipoItemCatalogo.SERVICO);
        servico.setNome("Consulta");
        servico.setValorBase(valorBase);
        servico.setStatus(StatusItem.ATIVO);
        return servico;
    }

    private ItemAtendimento itemCatalogo(BigDecimal valorTotal) {
        ItemAtendimento item = new ItemAtendimento();
        item.setId(1L);
        item.setAtendimentoId(1L);
        item.setCatalogoItemId(10L);
        item.setNomeItem("Curativo");
        item.setQuantidade(BigDecimal.ONE);
        item.setValorUnitario(valorTotal);
        item.setValorTotal(valorTotal);
        return item;
    }

    private ItemAtendimento itemMedicamento(BigDecimal valorTotal) {
        ItemAtendimento item = new ItemAtendimento();
        item.setId(2L);
        item.setAtendimentoId(1L);
        item.setMedicamentoId(20L);
        item.setNomeItem("Dipirona");
        item.setQuantidade(BigDecimal.ONE);
        item.setValorUnitario(valorTotal);
        item.setValorTotal(valorTotal);
        return item;
    }
}
