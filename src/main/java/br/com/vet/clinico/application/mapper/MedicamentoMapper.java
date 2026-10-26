package br.com.vet.clinico.application.mapper;

import br.com.vet.clinico.application.dto.request.AtualizarMedicamentoRequest;
import br.com.vet.clinico.application.dto.request.MedicamentoRequest;
import br.com.vet.clinico.application.dto.response.MedicamentoResponse;
import br.com.vet.clinico.domain.enums.StatusItem;
import br.com.vet.clinico.domain.model.Medicamento;
import org.springframework.stereotype.Component;

@Component
public class MedicamentoMapper {

    public Medicamento toEntity(MedicamentoRequest request) {
        Medicamento medicamento = new Medicamento();
        medicamento.setNome(request.nome().trim());
        medicamento.setDescricao(normalizarDescricao(request.descricao()));
        medicamento.setValorBase(request.valorBase());
        medicamento.setStatus(StatusItem.ATIVO);
        return medicamento;
    }

    public void updateEntity(Medicamento medicamento, AtualizarMedicamentoRequest request) {
        if (request.nome() != null && !request.nome().isBlank()) {
            medicamento.setNome(request.nome().trim());
        }
        if (request.descricao() != null) {
            medicamento.setDescricao(normalizarDescricao(request.descricao()));
        }
        if (request.valorBase() != null) {
            medicamento.setValorBase(request.valorBase());
        }
    }

    public MedicamentoResponse toResponse(Medicamento medicamento) {
        return new MedicamentoResponse(
                medicamento.getId(),
                medicamento.getNome(),
                medicamento.getDescricao(),
                medicamento.getValorBase(),
                medicamento.getStatus().name(),
                medicamento.getCriadoEm(),
                medicamento.getAtualizadoEm()
        );
    }

    private String normalizarDescricao(String descricao) {
        if (descricao == null) {
            return null;
        }
        String normalizada = descricao.trim();
        return normalizada.isEmpty() ? null : normalizada;
    }
}
