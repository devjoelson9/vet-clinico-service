package br.com.vet.clinico.application.mapper;

import br.com.vet.clinico.application.dto.request.AtualizarPrescricaoRequest;
import br.com.vet.clinico.application.dto.request.ItemPrescricaoRequest;
import br.com.vet.clinico.application.dto.request.RegistrarPrescricaoRequest;
import br.com.vet.clinico.application.dto.response.ItemPrescricaoResponse;
import br.com.vet.clinico.application.dto.response.PrescricaoResponse;
import br.com.vet.clinico.domain.model.ItemPrescricao;
import br.com.vet.clinico.domain.model.Prescricao;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PrescricaoMapper {

    public Prescricao toEntity(Long atendimentoId, RegistrarPrescricaoRequest request) {
        Prescricao prescricao = new Prescricao();
        prescricao.setAtendimentoId(atendimentoId);
        prescricao.setDataEmissao(request.dataEmissao());
        prescricao.setOrientacoesGerais(request.orientacoesGerais());
        return prescricao;
    }

    public void updateEntity(Prescricao prescricao, AtualizarPrescricaoRequest request) {
        if (request.dataEmissao() != null) {
            prescricao.setDataEmissao(request.dataEmissao());
        }
        if (request.orientacoesGerais() != null) {
            prescricao.setOrientacoesGerais(request.orientacoesGerais());
        }
    }

    public ItemPrescricao toEntity(Long prescricaoId, String nomeMedicamento, ItemPrescricaoRequest request) {
        ItemPrescricao item = new ItemPrescricao();
        item.setPrescricaoId(prescricaoId);
        item.setMedicamentoId(request.medicamentoId());
        item.setNomeMedicamento(nomeMedicamento);
        item.setDosagemConcentracao(request.dosagemConcentracao());
        item.setPosologia(request.posologia());
        item.setDuracaoTratamento(request.duracaoTratamento());
        item.setViaAdministracao(request.viaAdministracao());
        return item;
    }

    public PrescricaoResponse toResponse(Prescricao prescricao, List<ItemPrescricao> itens) {
        return new PrescricaoResponse(
                prescricao.getId(),
                prescricao.getAtendimentoId(),
                prescricao.getDataEmissao(),
                prescricao.getOrientacoesGerais(),
                prescricao.getCriadoEm(),
                itens.stream().map(this::toResponse).toList()
        );
    }

    public ItemPrescricaoResponse toResponse(ItemPrescricao item) {
        return new ItemPrescricaoResponse(
                item.getId(),
                item.getPrescricaoId(),
                item.getMedicamentoId(),
                item.getNomeMedicamento(),
                item.getDosagemConcentracao(),
                item.getPosologia(),
                item.getDuracaoTratamento(),
                item.getViaAdministracao(),
                item.getCriadoEm()
        );
    }
}
