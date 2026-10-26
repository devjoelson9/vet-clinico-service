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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogoItemServiceTest {

    @Mock
    private CatalogoItemRepository repository;

    private CatalogoItemService service;

    @BeforeEach
    void setUp() {
        service = new CatalogoItemService(repository, new CatalogoItemMapper());
    }

    @Test
    void deveCadastrarItemComStatusAtivo() {
        CatalogoItemRequest request = new CatalogoItemRequest(
                TipoItemCatalogo.SERVICO, "  Consulta  ", "Consulta clínica", new BigDecimal("100.00"));
        when(repository.existsByTipoAndNomeIgnoreCaseAndDeletadoEmIsNull(TipoItemCatalogo.SERVICO, "Consulta"))
                .thenReturn(false);
        when(repository.save(any(CatalogoItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CatalogoItemResponse response = service.cadastrar(request);

        assertThat(response.nome()).isEqualTo("Consulta");
        assertThat(response.status()).isEqualTo(StatusItem.ATIVO.name());
    }

    @Test
    void deveRecusarCadastroComNomeDuplicado() {
        CatalogoItemRequest request = new CatalogoItemRequest(
                TipoItemCatalogo.SERVICO, "Consulta", null, new BigDecimal("100.00"));
        when(repository.existsByTipoAndNomeIgnoreCaseAndDeletadoEmIsNull(TipoItemCatalogo.SERVICO, "Consulta"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.cadastrar(request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void deveRecusarAtualizacaoParaNomeDuplicado() {
        CatalogoItem item = itemExistente();
        when(repository.findByIdAndDeletadoEmIsNull(1L)).thenReturn(Optional.of(item));
        when(repository.existsByTipoAndNomeIgnoreCaseAndDeletadoEmIsNullAndIdNot(
                TipoItemCatalogo.SERVICO, "Exame", 1L)).thenReturn(true);

        AtualizarCatalogoItemRequest request = new AtualizarCatalogoItemRequest(
                "Exame", null, new BigDecimal("50.00"));

        assertThatThrownBy(() -> service.atualizar(1L, request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void deveExcluirLogicamenteEInativarItem() {
        CatalogoItem item = itemExistente();
        when(repository.findByIdAndDeletadoEmIsNull(1L)).thenReturn(Optional.of(item));
        when(repository.save(any(CatalogoItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.excluir(1L);

        ArgumentCaptor<CatalogoItem> captor = ArgumentCaptor.forClass(CatalogoItem.class);
        org.mockito.Mockito.verify(repository).save(captor.capture());
        assertThat(captor.getValue().getDeletadoEm()).isNotNull();
        assertThat(captor.getValue().getStatus()).isEqualTo(StatusItem.INATIVO);
    }

    @Test
    void deveRetornarErroQuandoItemNaoExistir() {
        when(repository.findByIdAndDeletadoEmIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private CatalogoItem itemExistente() {
        CatalogoItem item = new CatalogoItem();
        item.setId(1L);
        item.setTipo(TipoItemCatalogo.SERVICO);
        item.setNome("Consulta");
        item.setValorBase(new BigDecimal("100.00"));
        item.setStatus(StatusItem.ATIVO);
        return item;
    }
}
