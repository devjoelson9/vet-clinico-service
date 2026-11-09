package br.com.vet.clinico.application.service;

import br.com.vet.clinico.application.dto.request.RegistrarItemAtendimentoRequest;
import br.com.vet.clinico.application.dto.response.ItemAtendimentoResponse;
import br.com.vet.clinico.application.exception.BusinessException;
import br.com.vet.clinico.application.exception.ResourceNotFoundException;
import br.com.vet.clinico.application.mapper.ItemAtendimentoMapper;
import br.com.vet.clinico.domain.enums.StatusAtendimento;
import br.com.vet.clinico.domain.enums.StatusItem;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class ItemAtendimentoServiceTest {

    @Mock
    private ItemAtendimentoRepository repository;

    @Mock
    private AtendimentoRepository atendimentoRepository;

    @Mock
    private CatalogoItemRepository catalogoItemRepository;

    @Mock
    private MedicamentoRepository medicamentoRepository;

    private ItemAtendimentoService service;

    private final UsuarioAutenticado veterinario =
            new UsuarioAutenticado(10L, "vet@teste.com", Set.of("VETERINARIO"));

    @BeforeEach
    void setUp() {
        service = new ItemAtendimentoService(
                repository,
                atendimentoRepository,
                catalogoItemRepository,
                medicamentoRepository,
                new ItemAtendimentoMapper()
        );
    }

    @Test
    void deveRegistrarItemDoCatalogoComSnapshotENaoConsolidarAtendimento() {
        when(atendimentoRepository.findByIdAndDeletadoEmIsNull(1L))
                .thenReturn(Optional.of(atendimentoEmAndamento()));
        when(catalogoItemRepository.findByIdAndDeletadoEmIsNull(5L))
                .thenReturn(Optional.of(catalogoItemAtivo()));
        when(repository.save(any(ItemAtendimento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegistrarItemAtendimentoRequest request = new RegistrarItemAtendimentoRequest(
                TipoItemAtendimento.CATALOGO_ITEM, 5L, new BigDecimal("2.50"));

        ItemAtendimentoResponse response = service.registrar(1L, request, veterinario);

        assertThat(response.tipo()).isEqualTo(TipoItemAtendimento.CATALOGO_ITEM);
        assertThat(response.catalogoItemId()).isEqualTo(5L);
        assertThat(response.medicamentoId()).isNull();
        assertThat(response.nomeItem()).isEqualTo("Curativo");
        assertThat(response.quantidade()).isEqualByComparingTo("2.50");
        assertThat(response.valorUnitario()).isEqualByComparingTo("30.00");
        assertThat(response.valorTotal()).isEqualByComparingTo("75.00");

        ArgumentCaptor<ItemAtendimento> captor = ArgumentCaptor.forClass(ItemAtendimento.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getAtendimentoId()).isEqualTo(1L);
        verify(atendimentoRepository).findByIdAndDeletadoEmIsNull(1L);
        verify(atendimentoRepository, never()).save(any(Atendimento.class));
    }

    @Test
    void deveRegistrarMedicamentoComSnapshot() {
        when(atendimentoRepository.findByIdAndDeletadoEmIsNull(1L))
                .thenReturn(Optional.of(atendimentoEmAndamento()));
        when(medicamentoRepository.findByIdAndDeletadoEmIsNull(7L))
                .thenReturn(Optional.of(medicamentoAtivo()));
        when(repository.save(any(ItemAtendimento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegistrarItemAtendimentoRequest request = new RegistrarItemAtendimentoRequest(
                TipoItemAtendimento.MEDICAMENTO, 7L, new BigDecimal("3"));

        ItemAtendimentoResponse response = service.registrar(1L, request, veterinario);

        assertThat(response.tipo()).isEqualTo(TipoItemAtendimento.MEDICAMENTO);
        assertThat(response.catalogoItemId()).isNull();
        assertThat(response.medicamentoId()).isEqualTo(7L);
        assertThat(response.nomeItem()).isEqualTo("Dipirona");
        assertThat(response.valorUnitario()).isEqualByComparingTo("12.50");
        assertThat(response.valorTotal()).isEqualByComparingTo("37.50");
    }

    @Test
    void deveRecusarItemEmAtendimentoFinalizado() {
        Atendimento atendimento = atendimentoEmAndamento();
        atendimento.setStatus(StatusAtendimento.FINALIZADO);
        when(atendimentoRepository.findByIdAndDeletadoEmIsNull(1L)).thenReturn(Optional.of(atendimento));

        RegistrarItemAtendimentoRequest request = new RegistrarItemAtendimentoRequest(
                TipoItemAtendimento.CATALOGO_ITEM, 5L, BigDecimal.ONE);

        assertThatThrownBy(() -> service.registrar(1L, request, veterinario))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não está em andamento");

        verifyNoInteractions(catalogoItemRepository);
        verify(repository, never()).save(any(ItemAtendimento.class));
    }

    @Test
    void deveRecusarItemPorOutroVeterinario() {
        when(atendimentoRepository.findByIdAndDeletadoEmIsNull(1L))
                .thenReturn(Optional.of(atendimentoEmAndamento()));
        UsuarioAutenticado outro = new UsuarioAutenticado(99L, "outro@teste.com", Set.of("VETERINARIO"));

        RegistrarItemAtendimentoRequest request = new RegistrarItemAtendimentoRequest(
                TipoItemAtendimento.CATALOGO_ITEM, 5L, BigDecimal.ONE);

        assertThatThrownBy(() -> service.registrar(1L, request, outro))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(catalogoItemRepository, medicamentoRepository);
    }

    @Test
    void devePermitirItemPorAdmin() {
        when(atendimentoRepository.findByIdAndDeletadoEmIsNull(1L))
                .thenReturn(Optional.of(atendimentoEmAndamento()));
        when(catalogoItemRepository.findByIdAndDeletadoEmIsNull(5L))
                .thenReturn(Optional.of(catalogoItemAtivo()));
        when(repository.save(any(ItemAtendimento.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UsuarioAutenticado admin = new UsuarioAutenticado(1L, "admin@teste.com", Set.of("ADMIN"));

        ItemAtendimentoResponse response = service.registrar(
                1L,
                new RegistrarItemAtendimentoRequest(TipoItemAtendimento.CATALOGO_ITEM, 5L, BigDecimal.ONE),
                admin
        );

        assertThat(response.nomeItem()).isEqualTo("Curativo");
    }

    @Test
    void deveRecusarItemDeCatalogoInexistente() {
        when(atendimentoRepository.findByIdAndDeletadoEmIsNull(1L))
                .thenReturn(Optional.of(atendimentoEmAndamento()));
        when(catalogoItemRepository.findByIdAndDeletadoEmIsNull(99L)).thenReturn(Optional.empty());

        RegistrarItemAtendimentoRequest request = new RegistrarItemAtendimentoRequest(
                TipoItemAtendimento.CATALOGO_ITEM, 99L, BigDecimal.ONE);

        assertThatThrownBy(() -> service.registrar(1L, request, veterinario))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Item de catálogo não encontrado");
    }

    @Test
    void deveRecusarItemDeCatalogoInativo() {
        CatalogoItem item = catalogoItemAtivo();
        item.setStatus(StatusItem.INATIVO);
        when(atendimentoRepository.findByIdAndDeletadoEmIsNull(1L))
                .thenReturn(Optional.of(atendimentoEmAndamento()));
        when(catalogoItemRepository.findByIdAndDeletadoEmIsNull(5L)).thenReturn(Optional.of(item));

        RegistrarItemAtendimentoRequest request = new RegistrarItemAtendimentoRequest(
                TipoItemAtendimento.CATALOGO_ITEM, 5L, BigDecimal.ONE);

        assertThatThrownBy(() -> service.registrar(1L, request, veterinario))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inativo");
    }

    @Test
    void deveListarItensAtivosDoAtendimento() {
        when(atendimentoRepository.findByIdAndDeletadoEmIsNull(1L))
                .thenReturn(Optional.of(atendimentoEmAndamento()));
        when(repository.findByAtendimentoIdAndDeletadoEmIsNullOrderByCriadoEmAsc(1L))
                .thenReturn(List.of(itemPersistido()));

        List<ItemAtendimentoResponse> itens = service.listar(1L);

        assertThat(itens).hasSize(1);
        assertThat(itens.get(0).nomeItem()).isEqualTo("Curativo");
        assertThat(itens.get(0).tipo()).isEqualTo(TipoItemAtendimento.CATALOGO_ITEM);
    }

    @Test
    void deveRecusarListagemDeAtendimentoInexistente() {
        when(atendimentoRepository.findByIdAndDeletadoEmIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listar(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(repository);
    }

    @Test
    void deveExcluirItemLogicamente() {
        ItemAtendimento item = itemPersistido();
        when(atendimentoRepository.findByIdAndDeletadoEmIsNull(1L))
                .thenReturn(Optional.of(atendimentoEmAndamento()));
        when(repository.findByIdAndAtendimentoIdAndDeletadoEmIsNull(9L, 1L))
                .thenReturn(Optional.of(item));
        when(repository.save(any(ItemAtendimento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.excluir(1L, 9L, veterinario);

        assertThat(item.getDeletadoEm()).isNotNull();
        verify(repository).save(item);
    }

    @Test
    void deveRecusarExclusaoEmAtendimentoFinalizado() {
        Atendimento atendimento = atendimentoEmAndamento();
        atendimento.setStatus(StatusAtendimento.FINALIZADO);
        when(atendimentoRepository.findByIdAndDeletadoEmIsNull(1L)).thenReturn(Optional.of(atendimento));

        assertThatThrownBy(() -> service.excluir(1L, 9L, veterinario))
                .isInstanceOf(BusinessException.class);

        verify(repository, never()).findByIdAndAtendimentoIdAndDeletadoEmIsNull(9L, 1L);
    }

    @Test
    void deveRecusarExclusaoDeItemInexistente() {
        when(atendimentoRepository.findByIdAndDeletadoEmIsNull(1L))
                .thenReturn(Optional.of(atendimentoEmAndamento()));
        when(repository.findByIdAndAtendimentoIdAndDeletadoEmIsNull(9L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.excluir(1L, 9L, veterinario))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Atendimento atendimentoEmAndamento() {
        Atendimento atendimento = new Atendimento();
        atendimento.setId(1L);
        atendimento.setVeterinarioId(10L);
        atendimento.setStatus(StatusAtendimento.EM_ANDAMENTO);
        return atendimento;
    }

    private CatalogoItem catalogoItemAtivo() {
        CatalogoItem item = new CatalogoItem();
        item.setId(5L);
        item.setNome("Curativo");
        item.setValorBase(new BigDecimal("30.00"));
        item.setStatus(StatusItem.ATIVO);
        return item;
    }

    private Medicamento medicamentoAtivo() {
        Medicamento medicamento = new Medicamento();
        medicamento.setId(7L);
        medicamento.setNome("Dipirona");
        medicamento.setValorBase(new BigDecimal("12.50"));
        medicamento.setStatus(StatusItem.ATIVO);
        return medicamento;
    }

    private ItemAtendimento itemPersistido() {
        ItemAtendimento item = new ItemAtendimento();
        item.setId(9L);
        item.setAtendimentoId(1L);
        item.setCatalogoItemId(5L);
        item.setNomeItem("Curativo");
        item.setQuantidade(new BigDecimal("2.00"));
        item.setValorUnitario(new BigDecimal("30.00"));
        item.setValorTotal(new BigDecimal("60.00"));
        return item;
    }
}
