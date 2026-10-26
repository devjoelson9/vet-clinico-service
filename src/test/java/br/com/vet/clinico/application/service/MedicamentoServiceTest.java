package br.com.vet.clinico.application.service;

import br.com.vet.clinico.application.dto.request.MedicamentoRequest;
import br.com.vet.clinico.application.dto.response.MedicamentoResponse;
import br.com.vet.clinico.application.exception.ConflictException;
import br.com.vet.clinico.application.exception.ResourceNotFoundException;
import br.com.vet.clinico.application.mapper.MedicamentoMapper;
import br.com.vet.clinico.domain.enums.StatusItem;
import br.com.vet.clinico.domain.model.Medicamento;
import br.com.vet.clinico.domain.repository.MedicamentoRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicamentoServiceTest {

    @Mock
    private MedicamentoRepository repository;

    private MedicamentoService service;

    @BeforeEach
    void setUp() {
        service = new MedicamentoService(repository, new MedicamentoMapper());
    }

    @Test
    void deveCadastrarMedicamentoComStatusAtivo() {
        MedicamentoRequest request = new MedicamentoRequest(
                "  Dipirona  ", "Analgésico", new BigDecimal("12.50"));
        when(repository.existsByNomeIgnoreCaseAndDeletadoEmIsNull("Dipirona")).thenReturn(false);
        when(repository.save(any(Medicamento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MedicamentoResponse response = service.cadastrar(request);

        assertThat(response.nome()).isEqualTo("Dipirona");
        assertThat(response.status()).isEqualTo(StatusItem.ATIVO.name());
    }

    @Test
    void deveRecusarCadastroComNomeDuplicado() {
        MedicamentoRequest request = new MedicamentoRequest(
                "Dipirona", null, new BigDecimal("12.50"));
        when(repository.existsByNomeIgnoreCaseAndDeletadoEmIsNull("Dipirona")).thenReturn(true);

        assertThatThrownBy(() -> service.cadastrar(request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void deveExcluirLogicamenteEInativarMedicamento() {
        Medicamento medicamento = medicamentoExistente();
        when(repository.findByIdAndDeletadoEmIsNull(1L)).thenReturn(Optional.of(medicamento));
        when(repository.save(any(Medicamento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.excluir(1L);

        ArgumentCaptor<Medicamento> captor = ArgumentCaptor.forClass(Medicamento.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getDeletadoEm()).isNotNull();
        assertThat(captor.getValue().getStatus()).isEqualTo(StatusItem.INATIVO);
    }

    @Test
    void deveRetornarErroQuandoMedicamentoNaoExistir() {
        when(repository.findByIdAndDeletadoEmIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Medicamento medicamentoExistente() {
        Medicamento medicamento = new Medicamento();
        medicamento.setId(1L);
        medicamento.setNome("Dipirona");
        medicamento.setValorBase(new BigDecimal("12.50"));
        medicamento.setStatus(StatusItem.ATIVO);
        return medicamento;
    }
}
